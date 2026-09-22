package com.serratocreations.phovo.core.workmanager

/** Observable snapshot of a piece of unique work. */
data class WorkInfo(
    val uniqueWorkName: String,
    val state: WorkState,
    val runAttemptCount: Int,
    val tags: Set<String>
)

enum class WorkState {
    /** Waiting for its constraints, its delay, or a platform execution window. */
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    /** Whether no further runs will happen. Periodic work never reaches a finished state. */
    val isFinished: Boolean
        get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
}
