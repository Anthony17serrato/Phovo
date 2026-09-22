package com.serratocreations.phovo.core.workmanager

import android.content.Context
import android.os.Build
import androidx.work.Data
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequest as AndroidxOneTimeWorkRequest
import androidx.work.PeriodicWorkRequest as AndroidxPeriodicWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Android [PhovoWorkManager], delegating to androidx.work. Persistence, constraint monitoring and
 * backoff are all WorkManager's own; this class only translates between the two vocabularies.
 */
internal class AndroidPhovoWorkManager(
    context: Context
) : PhovoWorkManager {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueueUniqueWork(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    ) {
        val builder = AndroidxOneTimeWorkRequest.Builder(PhovoDelegatingWorker::class.java)
            .setInputData(request.workerId.toInputData(request.longRunning))
            .setConstraints(request.constraints.toAndroidx())
            .setBackoffCriteria(request.backoffPolicy.toAndroidx(), request.backoffDelay.toJavaDuration())

        if (request.initialDelay > Duration.ZERO) {
            builder.setInitialDelay(request.initialDelay.toJavaDuration())
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

        builder.setInputData(request.workerId.toInputData())
            .setConstraints(request.constraints.toAndroidx())
            .setBackoffCriteria(request.backoffPolicy.toAndroidx(), request.backoffDelay.toJavaDuration())

        if (request.initialDelay > Duration.ZERO) {
            builder.setInitialDelay(request.initialDelay.toJavaDuration())
        }
        request.tags.toAndroidxTags().forEach(builder::addTag)

        workManager.enqueueUniquePeriodicWork(uniqueWorkName, policy.toAndroidx(), builder.build())
    }

    override fun cancelUniqueWork(uniqueWorkName: String) {
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    override fun cancelAllWorkByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag.toAndroidxTag())
    }

    override fun getWorkInfoFlow(uniqueWorkName: String): Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName)
            .map { infos -> infos.firstOrNull()?.toPhovo(uniqueWorkName) }

    override suspend fun getWorkInfo(uniqueWorkName: String): WorkInfo? =
        getWorkInfoFlow(uniqueWorkName).first()

    private fun String.toInputData(longRunning: LongRunningInfo? = null): Data =
        Data.Builder()
            .putString(PhovoDelegatingWorker.KEY_WORKER_ID, this)
            .apply {
                if (longRunning != null) {
                    putString(PhovoDelegatingWorker.KEY_LONG_RUNNING_TITLE, longRunning.title)
                    putString(PhovoDelegatingWorker.KEY_LONG_RUNNING_SUBTITLE, longRunning.subtitle)
                }
            }
            .build()
}
