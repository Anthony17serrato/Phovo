package com.serratocreations.phovo.core.workmanager

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.koin.mp.KoinPlatformTools

/**
 * The single [CoroutineWorker] every Phovo work request is scheduled against.
 *
 * WorkManager instantiates workers reflectively with only a Context and WorkerParameters, so the
 * real worker is named by [KEY_WORKER_ID] in the input data and resolved through [WorkerRegistry]
 * at run time. The registry is read out of the global Koin context rather than through a custom
 * WorkerFactory: PhovoApplication starts Koin at process start, so the graph is always up before
 * WorkManager runs anything, and this avoids having to switch WorkManager to on demand
 * initialization and strip its InitializationProvider from the merged manifest.
 */
internal class PhovoDelegatingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val logger = PhovoLogger.withTag("PhovoDelegatingWorker")
    private val notifications by lazy { WorkNotifications(applicationContext) }

    private val longRunningTitle: String? get() = inputData.getString(KEY_LONG_RUNNING_TITLE)

    /**
     * WorkManager calls this itself for expedited work on API 30 and below. That path is
     * unreachable here, since AndroidPhovoWorkManager only marks work expedited on API 31+, but
     * implementing it keeps the default (which throws) out of the picture.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo = notifications.foregroundInfo(
        workerId = inputData.getString(KEY_WORKER_ID).orEmpty(),
        title = longRunningTitle.orEmpty(),
        subtitle = inputData.getString(KEY_LONG_RUNNING_SUBTITLE).orEmpty(),
        completed = 0,
        total = 0
    )

    override suspend fun doWork(): Result {
        val workerId = inputData.getString(KEY_WORKER_ID)
        if (workerId == null) {
            logger.e { "Work enqueued without a $KEY_WORKER_ID, failing." }
            return Result.failure()
        }

        val registry = KoinPlatformTools.defaultContext().get().get<WorkerRegistry>()
        val worker = registry.create(workerId)
        if (worker == null) {
            // The worker was renamed or removed while work referencing it was still on disk.
            // Failing is the only safe option; retrying would spin forever.
            logger.e { "No worker registered for id '$workerId', failing." }
            return Result.failure()
        }

        val title = longRunningTitle
        val subtitle = inputData.getString(KEY_LONG_RUNNING_SUBTITLE).orEmpty()
        if (title != null) {
            // Promotes this run to a foreground service, which is what lifts the ten minute
            // JobScheduler deadline. Android 15+ still budgets dataSync services per day.
            try {
                setForeground(notifications.foregroundInfo(workerId, title, subtitle, 0, 0))
            } catch (e: IllegalStateException) {
                if (!e.isForegroundStartDenied()) throw e
                // Measured on device: allowed when enqueued as the app leaves the foreground
                // (activity visibility grace period), refused when the run starts with the app
                // already in the background.
                logger.w { "Foreground service refused for '$workerId'; handing to onLongRunningDenied." }
                return worker.onLongRunningDenied().toAndroidx()
            }
        }

        // Handed off by AndroidPhovoWorkManager as the user left: the run is already going
        // in-process, so this job only lends it the foreground service.
        val run = inputData.getString(KEY_UNIQUE_WORK_NAME)
            ?.let { KoinPlatformTools.defaultContext().get().get<ForegroundRuns>().active(it) }
        if (run != null) return join(run, workerId, title, subtitle)

        if (title != null) {
            // Only the setForeground above starts the service. These just replace its notification.
            worker.progressReporter = WorkProgressReporter { completed, total ->
                setForeground(notifications.foregroundInfo(workerId, title, subtitle, completed, total))
            }
        }

        return try {
            worker.doWork().toAndroidx()
        } finally {
            worker.progressReporter = null
        }
    }

    /**
     * Waits for an in-process [run] and returns its result. Cancelling this job, which
     * AndroidPhovoWorkManager does when the user comes back, ends the wait, not the run.
     */
    private suspend fun join(run: ForegroundRuns.Run, workerId: String, title: String?, subtitle: String): Result {
        run.joined = true
        if (title != null) {
            val reporter = WorkProgressReporter { completed, total ->
                try {
                    setForeground(notifications.foregroundInfo(workerId, title, subtitle, completed, total))
                } catch (e: CancellationException) {
                    // This job being stopped must not cancel the run, which is the caller here.
                    if (!currentCoroutineContext().isActive) throw e
                } catch (e: IllegalStateException) {
                    logger.w { "Progress update for '$workerId' dropped: ${e.message}" }
                }
            }
            run.worker.progressReporter = reporter
            // The run may have gone quiet before the hand-off; show where it got to.
            run.worker.lastReportedProgress?.let { reporter.report(it.completed, it.total) }
        }
        try {
            run.result.join()
        } finally {
            run.joined = false
            run.worker.progressReporter = null
        }
        if (run.result.isCancelled) return Result.failure()
        @OptIn(ExperimentalCoroutinesApi::class)
        return run.result.getCompleted().toAndroidx()
    }

    private fun WorkResult.toAndroidx(): Result = when (this) {
        is WorkResult.Success -> Result.success()
        is WorkResult.Retry -> Result.retry()
        is WorkResult.Failure -> Result.failure()
    }

    // ForegroundServiceStartNotAllowedException is API 31 and minSdk is lower, so it is caught as
    // its IllegalStateException superclass and only named behind the version check.
    private fun Throwable.isForegroundStartDenied(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (this is ForegroundServiceStartNotAllowedException ||
                cause is ForegroundServiceStartNotAllowedException)

    companion object {
        const val KEY_WORKER_ID = "phovo.work.workerId"
        const val KEY_UNIQUE_WORK_NAME = "phovo.work.uniqueWorkName"
        const val KEY_LONG_RUNNING_TITLE = "phovo.work.longRunning.title"
        const val KEY_LONG_RUNNING_SUBTITLE = "phovo.work.longRunning.subtitle"
    }
}
