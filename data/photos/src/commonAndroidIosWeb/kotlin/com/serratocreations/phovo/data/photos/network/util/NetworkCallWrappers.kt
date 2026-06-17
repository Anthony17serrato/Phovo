package com.serratocreations.phovo.data.photos.network.util

import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkResult
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.io.IOException

suspend fun <T> networkResultCallWrapper(
    retryPolicy: NetworkCallRetryPolicy = NetworkCallRetryPolicy.NONE,
    networkCall: suspend () -> NetworkResult<T>
): NetworkResult<T> {
    suspend fun getResult() = try {
        networkCall()
    } catch (e: Exception) {
        when (e) {
            is IOException -> NetworkResult.NetworkError(message = "$e")
            else -> throw e
        }
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
            val message = "Network call failed with status: ${result.status}"
            NetworkResult.NetworkError(message = message)
        }
    }