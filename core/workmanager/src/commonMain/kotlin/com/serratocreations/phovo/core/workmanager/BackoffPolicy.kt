package com.serratocreations.phovo.core.workmanager

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How the delay before a [WorkResult.Retry] grows with each failed attempt. */
enum class BackoffPolicy {
    /** delay * 2^(attempts - 1) */
    EXPONENTIAL,

    /** delay * attempts */
    LINEAR;

    companion object {
        /** Matches androidx.work's WorkRequest.MIN_BACKOFF_MILLIS. */
        val MIN_BACKOFF_DELAY: Duration = 10.seconds

        /** Matches androidx.work's WorkRequest.MAX_BACKOFF_MILLIS. */
        val MAX_BACKOFF_DELAY: Duration = (5 * 60 * 60).seconds

        val DEFAULT_BACKOFF_DELAY: Duration = 30.seconds
    }
}

/**
 * Delay before attempt number [runAttemptCount] + 1, clamped to the same bounds androidx.work uses
 * so both platforms agree. [runAttemptCount] is the number of attempts that have already failed.
 *
 * Android computes this itself inside WorkManager; iOS calls this directly. It is kept in common
 * code so the two platforms cannot drift and so it can be unit tested without a device.
 */
fun BackoffPolicy.delayForAttempt(runAttemptCount: Int, backoffDelay: Duration): Duration {
    if (runAttemptCount <= 0) return Duration.ZERO
    val scaled = when (this) {
        // Cap the shift to avoid overflowing the multiplier on a worker that keeps retrying.
        BackoffPolicy.EXPONENTIAL -> backoffDelay * (1L shl minOf(runAttemptCount - 1, 32)).toDouble()
        BackoffPolicy.LINEAR -> backoffDelay * runAttemptCount.toDouble()
    }
    return scaled.coerceIn(BackoffPolicy.MIN_BACKOFF_DELAY, BackoffPolicy.MAX_BACKOFF_DELAY)
}
