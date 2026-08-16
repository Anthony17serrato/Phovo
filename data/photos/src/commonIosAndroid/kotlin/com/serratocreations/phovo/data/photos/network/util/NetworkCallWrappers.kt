package com.serratocreations.phovo.data.photos.network.util

import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkFailure
import com.serratocreations.phovo.core.model.network.NetworkResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Classifies a failed network call.
 *
 * Ktor's timeout exceptions all extend [IOException], so they are separated out first to
 * distinguish a slow server from an absent one. [SerializationException] is an
 * `IllegalArgumentException`, as is `UnresolvedAddressException` on JVM targets — neither is an
 * [IOException], which is why the catch below cannot be narrowed to that type.
 */
private fun classifyNetworkFailure(e: Throwable): NetworkFailure = when (e) {
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException -> NetworkFailure.Timeout
    is SerializationException -> NetworkFailure.Malformed
    is IOException -> NetworkFailure.Unreachable
    else -> NetworkFailure.Unknown
}

suspend fun <T> networkResultCallWrapper(
    retryPolicy: NetworkCallRetryPolicy = NetworkCallRetryPolicy.NONE,
    networkCall: suspend () -> NetworkResult<T>
): NetworkResult<T> {
    suspend fun getResult(): NetworkResult<T> = try {
        networkCall()
    } catch (e: CancellationException) {
        // Never convert cancellation into a failure result, it must propagate to the caller.
        throw e
    } catch (e: Exception) {
        // "$e" retains the exception type, which is the only diagnostic for NetworkFailure.Unknown.
        NetworkResult.NetworkError(message = "$e", failure = classifyNetworkFailure(e))
    }

    var result: NetworkResult<T> = getResult()
    if (result is NetworkResult.NetworkSuccess) return result

    repeat(retryPolicy.retryAttempts) { attemptIndex ->
        // TODO should not retry if error is network based and call is made inside of a WorkManager
        retryPolicy.executeDelay(attemptIndex)

        result = getResult()
        if (result is NetworkResult.NetworkSuccess) return result
    }

    return result
}

suspend fun networkCallWrapper(
    retryPolicy: NetworkCallRetryPolicy = NetworkCallRetryPolicy.NONE,
    networkCall: suspend () -> HttpResponse
): NetworkResult<HttpResponse> =
    networkResultCallWrapper(
        retryPolicy = retryPolicy
    ) {
        val result = networkCall()
        if (result.status.isSuccess()) {
            NetworkResult.NetworkSuccess(result)
        } else {
            NetworkResult.NetworkError(
                message = "Network call failed with status: ${result.status}",
                failure = NetworkFailure.HttpStatus,
                statusCode = result.status.value
            )
        }
    }
