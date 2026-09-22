package com.serratocreations.phovo.core.workmanager

/**
 * A unit of background work. Implementations must be safe to construct at any time, including in a
 * freshly started process that is only running to drain the work queue, so keep constructors cheap
 * and pull collaborators from DI rather than from whatever enqueued the work.
 *
 * [doWork] is called on a background dispatcher and may be cancelled when the platform revokes the
 * execution window, so it must be cooperative with cancellation.
 */
abstract class PhovoWorker {

    /**
     * Set by the runtime immediately before [doWork] and cleared afterwards. Null for work that was
     * not enqueued with a [LongRunningInfo], since there is no progress surface to drive.
     */
    internal var progressReporter: WorkProgressReporter? = null

    abstract suspend fun doWork(): WorkResult

    /**
     * Reports how far along this run is, updating the Android notification and the iOS system
     * progress UI.
     *
     * Only meaningful for work enqueued with a [LongRunningInfo]; it is a no-op otherwise. Call it
     * regularly from long-running work. iOS may forcibly expire a continued processing task that
     * appears stalled, so silence is not free.
     */
    protected suspend fun setProgress(completed: Long, total: Long) {
        require(completed >= 0 && total >= 0) { "Progress cannot be negative." }
        progressReporter?.report(completed, total)
    }
}

/** How [PhovoWorker.setProgress] reaches the platform's progress surface. */
internal fun interface WorkProgressReporter {
    suspend fun report(completed: Long, total: Long)
}

/** Outcome of a single [PhovoWorker.doWork] run. */
sealed interface WorkResult {
    /** The work finished. Periodic work is rescheduled, one time work is done. */
    data object Success : WorkResult

    /** The work did not finish but should be attempted again after the backoff delay. */
    data object Retry : WorkResult

    /** The work failed permanently and will not be attempted again. */
    data object Failure : WorkResult
}
