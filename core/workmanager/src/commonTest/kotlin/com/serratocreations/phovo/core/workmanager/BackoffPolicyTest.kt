package com.serratocreations.phovo.core.workmanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BackoffPolicyTest {

    @Test
    fun `no delay before the first attempt`() {
        assertEquals(
            Duration.ZERO,
            BackoffPolicy.EXPONENTIAL.delayForAttempt(runAttemptCount = 0, backoffDelay = 30.seconds)
        )
    }

    @Test
    fun `exponential doubles each attempt`() {
        val delay = 30.seconds
        assertEquals(30.seconds, BackoffPolicy.EXPONENTIAL.delayForAttempt(1, delay))
        assertEquals(60.seconds, BackoffPolicy.EXPONENTIAL.delayForAttempt(2, delay))
        assertEquals(120.seconds, BackoffPolicy.EXPONENTIAL.delayForAttempt(3, delay))
        assertEquals(240.seconds, BackoffPolicy.EXPONENTIAL.delayForAttempt(4, delay))
    }

    @Test
    fun `linear scales by attempt count`() {
        val delay = 30.seconds
        assertEquals(30.seconds, BackoffPolicy.LINEAR.delayForAttempt(1, delay))
        assertEquals(60.seconds, BackoffPolicy.LINEAR.delayForAttempt(2, delay))
        assertEquals(90.seconds, BackoffPolicy.LINEAR.delayForAttempt(3, delay))
    }

    @Test
    fun `delay below the floor is raised to it`() {
        assertEquals(
            BackoffPolicy.MIN_BACKOFF_DELAY,
            BackoffPolicy.LINEAR.delayForAttempt(1, backoffDelay = 1.seconds)
        )
    }

    @Test
    fun `delay is capped at five hours`() {
        assertEquals(5.hours, BackoffPolicy.MAX_BACKOFF_DELAY)
        assertEquals(
            BackoffPolicy.MAX_BACKOFF_DELAY,
            BackoffPolicy.EXPONENTIAL.delayForAttempt(runAttemptCount = 20, backoffDelay = 30.seconds)
        )
    }

    @Test
    fun `a worker that keeps retrying does not overflow the multiplier`() {
        // The shift is clamped, so a very high attempt count must still land on the cap rather
        // than wrapping to a negative or zero delay.
        assertEquals(
            BackoffPolicy.MAX_BACKOFF_DELAY,
            BackoffPolicy.EXPONENTIAL.delayForAttempt(runAttemptCount = 500, backoffDelay = 10.minutes)
        )
    }
}
