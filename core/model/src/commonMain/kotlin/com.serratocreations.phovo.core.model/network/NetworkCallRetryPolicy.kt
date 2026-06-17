package com.serratocreations.phovo.core.model.network

import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration

sealed interface NetworkCallRetryPolicy {
    /**
     * The number of attempts which will be made after the first failure
     */
    val retryAttempts: Int

    /**
     * Executes the delay/suspension for the current retry attempt.
     * @param attemptIndex The 0-based index of the current retry.
     */
    suspend fun executeDelay(attemptIndex: Int)

    data object NONE : NetworkCallRetryPolicy {
        override val retryAttempts: Int = 0
        override suspend fun executeDelay(attemptIndex: Int) {}
    }

    data class RetryAfterDelay(
        override val retryAttempts: Int = 3,
        val delayDuration: Duration
    ) : NetworkCallRetryPolicy {
        override suspend fun executeDelay(attemptIndex: Int) {
            delay(delayDuration)
        }
    }

    data class RetryAfterLambda(
        override val retryAttempts: Int = 3,
        val lambda: suspend (attemptIndex: Int) -> Unit
    ) : NetworkCallRetryPolicy {
        override suspend fun executeDelay(attemptIndex: Int) {
            lambda(attemptIndex)
        }
    }

    class ExponentialBackoff(
        override val retryAttempts: Int = 3,
        val initialDelay: Duration,
        val maxDelay: Duration,
        val multiplier: Double = 2.0
    ) : NetworkCallRetryPolicy {
        override suspend fun executeDelay(attemptIndex: Int) {
            val factor = multiplier.pow(attemptIndex.toDouble())
            val delayMs = (initialDelay.inWholeMilliseconds * factor).toLong()
            val finalDelayMs = minOf(delayMs, maxDelay.inWholeMilliseconds)

            val jitterFactor = Random.nextDouble(-0.1, 0.1)
            val jitter = (finalDelayMs * jitterFactor).toLong()

            delay((finalDelayMs + jitter).coerceAtLeast(0))
        }
    }
}