package com.serratocreations.phovo.core.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.serratocreations.phovo.core.logger.PhovoLogger
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
            setForeground(notifications.foregroundInfo(title, subtitle, 0, 0))
            worker.progressReporter = WorkProgressReporter { completed, total ->
                setForeground(notifications.foregroundInfo(title, subtitle, completed, total))
            }
        }

        return try {
            when (worker.doWork()) {
                is WorkResult.Success -> Result.success()
                is WorkResult.Retry -> Result.retry()
                is WorkResult.Failure -> Result.failure()
            }
        } finally {
            worker.progressReporter = null
        }
    }

    companion object {
        const val KEY_WORKER_ID = "phovo.work.workerId"
        const val KEY_LONG_RUNNING_TITLE = "phovo.work.longRunning.title"
        const val KEY_LONG_RUNNING_SUBTITLE = "phovo.work.longRunning.subtitle"
    }
}
