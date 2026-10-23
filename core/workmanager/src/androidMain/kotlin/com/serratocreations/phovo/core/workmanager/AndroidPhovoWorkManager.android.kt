package com.serratocreations.phovo.core.workmanager

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Data
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequest as AndroidxOneTimeWorkRequest
import androidx.work.PeriodicWorkRequest as AndroidxPeriodicWorkRequest
import androidx.work.WorkManager
import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Android [PhovoWorkManager], delegating to androidx.work. Persistence, constraint monitoring and
 * backoff are all WorkManager's own; this class mostly translates between the two vocabularies.
 *
 * The exception is long-running work enqueued while the app is in the foreground. That runs
 * in-process through [ForegroundRuns] and only becomes an androidx job, and so a foreground
 * service, if the user leaves while it is still going. It matches what IosPhovoWorkManager does,
 * and keeps the time the app is open out of the job quota that long-running workers count
 * against from Android 16.
 */
internal class AndroidPhovoWorkManager(
    context: Context,
    private val runs: ForegroundRuns,
    private val registry: Lazy<WorkerRegistry>,
    private val logger: PhovoLogger
) : PhovoWorkManager {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    @Volatile
    private var lifecycleStarted = false

    init {
        // ProcessLifecycleOwner only accepts observers on the main thread, and Koin may build this
        // singleton anywhere. Adding the observer replays ON_START straight away if the app is
        // already open.
        val observe = {
            ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        lifecycleStarted = true
                        takeBackHandedOffRuns()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        lifecycleStarted = false
                        handOffActiveRuns()
                    }
                    else -> Unit
                }
            })
        }
        if (Looper.myLooper() == Looper.getMainLooper()) observe() else Handler(Looper.getMainLooper()).post(observe)
    }

    /**
     * ProcessLifecycleOwner only reaches ON_START once the first activity starts, after
     * Application.onCreate, which is where startup work gets enqueued. A process launching an
     * activity already has foreground importance by then, and one WorkManager started in the
     * background does not, so that covers the gap.
     */
    private fun isForeground(): Boolean {
        if (lifecycleStarted) return true
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    /**
     * The user left with in-process runs still going: give each an androidx job that promotes to a
     * foreground service and joins it. Expedited so it starts inside the activity visibility
     * grace period, which is what lets the foreground service start at all. Measured on device.
     */
    private fun handOffActiveRuns() {
        runs.allActive().forEach { run ->
            run.handedOff = true
            enqueueAndroidx(
                uniqueWorkName = run.uniqueWorkName,
                policy = ExistingWorkPolicy.REPLACE,
                request = run.request.copy(expedited = true)
            )
        }
    }

    /** The user is back: the runs no longer need a foreground service. Only the joins stop. */
    private fun takeBackHandedOffRuns() {
        runs.allActive().filter { it.handedOff }.forEach { run ->
            run.handedOff = false
            workManager.cancelUniqueWork(run.uniqueWorkName)
        }
    }

    override fun enqueueUniqueWork(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    ) {
        // Constrained work goes to androidx.work even when long-running, so its constraints are
        // never skipped by running it here.
        if (request.longRunning != null && request.constraints == Constraints.NONE && isForeground()) {
            startInProcess(uniqueWorkName, policy, request)
            return
        }

        // An in-process run counts as existing work under the name.
        if (runs.active(uniqueWorkName) != null) {
            if (policy == ExistingWorkPolicy.KEEP) return
            runs.cancel(uniqueWorkName)
        }
        runs.forget(uniqueWorkName)
        enqueueAndroidx(uniqueWorkName, policy, request)
    }

    /**
     * KEEP only sees an in-process run as existing work. Work androidx.work still holds under the
     * name, such as a retry waiting out its backoff, is not checked: that would mean a blocking
     * read of WorkManager's database on the caller's thread.
     */
    private fun startInProcess(uniqueWorkName: String, policy: ExistingWorkPolicy, request: OneTimeWorkRequest) {
        if (runs.active(uniqueWorkName) != null) {
            if (policy == ExistingWorkPolicy.KEEP) return
            runs.cancel(uniqueWorkName)
        }
        if (policy == ExistingWorkPolicy.REPLACE) workManager.cancelUniqueWork(uniqueWorkName)

        val worker = registry.value.create(request.workerId)
        if (worker == null) {
            // Same outcome as PhovoDelegatingWorker would give the androidx job.
            logger.e { "No worker registered for id '${request.workerId}', failing '$uniqueWorkName'." }
            return
        }
        runs.start(uniqueWorkName, request, worker, ::onInProcessFinished)
    }

    private fun onInProcessFinished(run: ForegroundRuns.Run, result: WorkResult) {
        val name = run.uniqueWorkName
        when (result) {
            is WorkResult.Success -> runs.recordOutcome(run, WorkState.SUCCEEDED)
            is WorkResult.Failure -> runs.recordOutcome(run, WorkState.FAILED)
            is WorkResult.Retry -> {
                // A joined androidx job returns Retry itself, and WorkManager backs it off.
                // Otherwise WorkManager takes over from here, after the first backoff delay.
                if (!run.joined) {
                    runs.forget(name)
                    enqueueAndroidx(
                        uniqueWorkName = name,
                        policy = ExistingWorkPolicy.REPLACE,
                        request = run.request.copy(expedited = false),
                        initialDelay = run.request.backoffPolicy.delayForAttempt(1, run.request.backoffDelay)
                    )
                }
                return
            }
        }
        // A hand-off job that has not joined yet would otherwise run the worker a second time.
        if (run.handedOff && !run.joined) workManager.cancelUniqueWork(name)
    }

    private fun enqueueAndroidx(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
        initialDelay: Duration = request.initialDelay
    ) {
        val builder = AndroidxOneTimeWorkRequest.Builder(PhovoDelegatingWorker::class.java)
            .setInputData(request.workerId.toInputData(uniqueWorkName, request.longRunning))
            .setConstraints(request.constraints.toAndroidx())
            .setBackoffCriteria(request.backoffPolicy.toAndroidx(), request.backoffDelay.toJavaDuration())

        if (initialDelay > Duration.ZERO) {
            builder.setInitialDelay(initialDelay.toJavaDuration())
        }
        if (request.expedited && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Gated on API 31 deliberately. WorkForegroundKt bails out of the foreground service
            // path when SDK_INT >= 31, so getForegroundInfo() is never called and this module
            // needs no notification, no channel and no FOREGROUND_SERVICE_DATA_SYNC manifest
            // entry. Below 31 expedited work would be run as a foreground service instead, so we
            // simply enqueue it normally there; the process is alive and it starts quickly anyway.
            //
            // RUN_AS_NON_EXPEDITED_WORK_REQUEST rather than DROP_WORK_REQUEST: running late beats
            // silently discarding a sync the user asked for.
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        request.tags.toAndroidxTags().forEach(builder::addTag)

        workManager.enqueueUniqueWork(uniqueWorkName, policy.toAndroidx(), builder.build())
    }

    override fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        policy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest
    ) {
        val repeatInterval = request.repeatInterval
            .coerceAtLeast(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL)
            .toJavaDuration()
        val flexInterval = request.flexInterval?.toJavaDuration()

        val builder = if (flexInterval != null) {
            AndroidxPeriodicWorkRequest.Builder(
                PhovoDelegatingWorker::class.java,
                repeatInterval,
                flexInterval
            )
        } else {
            AndroidxPeriodicWorkRequest.Builder(PhovoDelegatingWorker::class.java, repeatInterval)
        }

        builder.setInputData(request.workerId.toInputData(uniqueWorkName))
            .setConstraints(request.constraints.toAndroidx())
            .setBackoffCriteria(request.backoffPolicy.toAndroidx(), request.backoffDelay.toJavaDuration())

        if (request.initialDelay > Duration.ZERO) {
            builder.setInitialDelay(request.initialDelay.toJavaDuration())
        }
        request.tags.toAndroidxTags().forEach(builder::addTag)

        workManager.enqueueUniquePeriodicWork(uniqueWorkName, policy.toAndroidx(), builder.build())
    }

    override fun cancelUniqueWork(uniqueWorkName: String) {
        runs.cancel(uniqueWorkName)
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    override fun cancelAllWorkByTag(tag: String) {
        runs.namesWithTag(tag).forEach(runs::cancel)
        workManager.cancelAllWorkByTag(tag.toAndroidxTag())
    }

    /** An in-process run, or the outcome of the last one, wins over androidx.work's record. */
    override fun getWorkInfoFlow(uniqueWorkName: String): Flow<WorkInfo?> =
        combine(
            runs.infoFlow(uniqueWorkName),
            workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName)
                .map { infos -> infos.firstOrNull()?.toPhovo(uniqueWorkName) }
        ) { inProcess, androidx -> inProcess ?: androidx }

    override suspend fun getWorkInfo(uniqueWorkName: String): WorkInfo? =
        getWorkInfoFlow(uniqueWorkName).first()

    private fun String.toInputData(uniqueWorkName: String, longRunning: LongRunningInfo? = null): Data =
        Data.Builder()
            .putString(PhovoDelegatingWorker.KEY_WORKER_ID, this)
            .putString(PhovoDelegatingWorker.KEY_UNIQUE_WORK_NAME, uniqueWorkName)
            .apply {
                if (longRunning != null) {
                    putString(PhovoDelegatingWorker.KEY_LONG_RUNNING_TITLE, longRunning.title)
                    putString(PhovoDelegatingWorker.KEY_LONG_RUNNING_SUBTITLE, longRunning.subtitle)
                }
            }
            .build()
}
