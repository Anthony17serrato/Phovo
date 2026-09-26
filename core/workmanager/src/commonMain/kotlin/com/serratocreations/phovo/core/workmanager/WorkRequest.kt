package com.serratocreations.phovo.core.workmanager

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** A description of work to enqueue. See [OneTimeWorkRequest] and [PeriodicWorkRequest]. */
sealed interface WorkRequest {
    /** Id of the [WorkerRegistration] that knows how to build the worker. */
    val workerId: String
    val constraints: Constraints
    val backoffPolicy: BackoffPolicy
    val backoffDelay: Duration
    val tags: Set<String>
}

/** Work that runs once, then is done. */
data class OneTimeWorkRequest(
    override val workerId: String,
    override val constraints: Constraints = Constraints.NONE,
    val initialDelay: Duration = Duration.ZERO,
    /**
     * Ask for this work to run as soon as possible rather than whenever the platform gets around
     * to it.
     *
     * The guarantee both platforms keep is: **expedited work starts promptly while the app is in
     * the foreground.** Neither platform promises anything for an app the user has left. Android
     * may additionally expedite in the background on API 31+, subject to the system's expedited
     * quota, and falls back to ordinary scheduling when that quota is spent. iOS cannot wake a
     * suspended app on demand at all, so there it means the work jumps the queue and the app asks
     * for a few extra seconds if the user leaves mid run.
     *
     * Expedited work cannot be delayed and cannot ask for power related constraints. See the
     * [init] block.
     *
     * Combining this with [longRunning] is allowed, because the two ask for different things:
     * this one is about when the work starts, [longRunning] is about how long it may run once
     * started. What each platform makes of the pair:
     *
     *  - **Android** schedules an expedited job that then promotes itself to a foreground service.
     *    Both apply. Worth weighing though: expedited quota is finite and meant for short urgent
     *    work, so spending it on something that is about to become a foreground service anyway is
     *    usually a poor trade.
     *  - **iOS 26+** ignores it. A continued processing task already starts immediately and
     *    outranks the ordinary queue, so there is nothing left for this flag to ask for.
     *  - **iOS below 26** honours it. Long-running work falls back to the ordinary queue there,
     *    where being expedited still moves it to the front.
     */
    val expedited: Boolean = false,
    /**
     * Run this as long-running work with a user-visible progress surface, lifting the platform's
     * ordinary execution deadline. See [LongRunningInfo] for what each platform does with it.
     *
     * This says nothing about when the work starts, only how long it may run. See [expedited] for
     * what combining the two means on each platform.
     */
    val longRunning: LongRunningInfo? = null,
    override val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    override val backoffDelay: Duration = BackoffPolicy.DEFAULT_BACKOFF_DELAY,
    override val tags: Set<String> = emptySet()
) : WorkRequest {
    init {
        if (expedited) {
            // Both rules come from androidx.work, which rejects these combinations when the
            // request is built. Enforcing them here turns an Android only crash into the same
            // error on every platform, at the call site that caused it.
            require(initialDelay == Duration.ZERO) {
                "Expedited work cannot be delayed, but '$workerId' asked for $initialDelay."
            }
            require(!constraints.requiresCharging && !constraints.requiresBatteryNotLow) {
                "Expedited work only supports network constraints, but '$workerId' asked for " +
                    "requiresCharging=${constraints.requiresCharging} " +
                    "requiresBatteryNotLow=${constraints.requiresBatteryNotLow}."
            }
        }
        if (longRunning != null) {
            // iOS ignores earliestBeginDate on a continued processing task and starts it at once.
            // Rejecting the delay outright beats honouring it on Android and dropping it on iOS.
            require(initialDelay == Duration.ZERO) {
                "Long-running work starts immediately and cannot be delayed, but '$workerId' " +
                    "asked for $initialDelay."
            }
        }
    }
}

/**
 * Work that repeats on an interval.
 *
 * Neither platform guarantees the interval. Android will not run periodic work more often than
 * [MIN_PERIODIC_INTERVAL], and iOS decides for itself when to grant background time, so treat
 * [repeatInterval] as a floor rather than a schedule.
 *
 * There is no expedited periodic work. androidx.work rejects it outright
 * ("PeriodicWorkRequests cannot be expedited"), so the option is not offered here either.
 */
data class PeriodicWorkRequest(
    override val workerId: String,
    val repeatInterval: Duration,
    /**
     * Trailing window of [repeatInterval] in which the work may run. Ignored on iOS, which does not
     * expose a flex concept.
     */
    val flexInterval: Duration? = null,
    override val constraints: Constraints = Constraints.NONE,
    val initialDelay: Duration = Duration.ZERO,
    override val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    override val backoffDelay: Duration = BackoffPolicy.DEFAULT_BACKOFF_DELAY,
    override val tags: Set<String> = emptySet()
) : WorkRequest {
    companion object {
        /** Matches androidx.work's PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS. */
        val MIN_PERIODIC_INTERVAL: Duration = 15.minutes
    }
}

/** What to do when unique one time work is enqueued under a name that already exists. */
enum class ExistingWorkPolicy {
    /** Cancel the existing work and enqueue the new request. */
    REPLACE,

    /** Keep the existing work and drop the new request, unless the existing work already finished. */
    KEEP
}

/** What to do when unique periodic work is enqueued under a name that already exists. */
enum class ExistingPeriodicWorkPolicy {
    /** Apply the new request to the existing schedule without resetting the next run time. */
    UPDATE,

    /** Keep the existing schedule and drop the new request. */
    KEEP,

    /** Cancel the existing schedule and start over from the new request. */
    CANCEL_AND_REENQUEUE
}
