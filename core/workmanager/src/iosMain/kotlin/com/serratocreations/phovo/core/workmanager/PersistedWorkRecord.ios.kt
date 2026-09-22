package com.serratocreations.phovo.core.workmanager

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * On disk form of a piece of unique work.
 *
 * iOS has no system provided work database, so this module keeps its own. Everything needed to run
 * the work in a cold process lives here, which is why the worker is stored as an id rather than an
 * instance. Fields are written to NSUserDefaults as JSON, so changing them is a format change.
 */
@Serializable
internal data class PersistedWorkRecord(
    val uniqueWorkName: String,
    val workerId: String,
    val periodic: Boolean,
    val repeatIntervalMillis: Long? = null,
    val requiredNetworkType: NetworkType = NetworkType.NONE,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
    /** Runs ahead of non expedited work and is scheduled with no earliestBeginDate. */
    val expedited: Boolean = false,
    /** Set when the work was enqueued with a [LongRunningInfo]. Drives the iOS 26 system UI. */
    val longRunningTitle: String? = null,
    val longRunningSubtitle: String? = null,
    val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    val backoffDelayMillis: Long,
    val tags: Set<String> = emptySet(),
    val earliestRunAtEpochMillis: Long,
    val runAttemptCount: Int = 0,
    val state: WorkState = WorkState.ENQUEUED
) {
    val constraints: Constraints
        get() = Constraints(requiredNetworkType, requiresCharging, requiresBatteryNotLow)

    val backoffDelay: Duration get() = backoffDelayMillis.milliseconds

    val repeatInterval: Duration? get() = repeatIntervalMillis?.milliseconds

    val longRunning: LongRunningInfo?
        get() = longRunningTitle?.let { LongRunningInfo(it, longRunningSubtitle.orEmpty()) }

    /** Ready to run as far as the clock is concerned. Constraints are checked separately. */
    fun isDue(nowEpochMillis: Long): Boolean =
        state == WorkState.ENQUEUED && earliestRunAtEpochMillis <= nowEpochMillis

    fun toWorkInfo(): WorkInfo = WorkInfo(
        uniqueWorkName = uniqueWorkName,
        state = state,
        runAttemptCount = runAttemptCount,
        tags = tags
    )
}
