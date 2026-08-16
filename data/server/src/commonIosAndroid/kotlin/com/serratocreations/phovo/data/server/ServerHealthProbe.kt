package com.serratocreations.phovo.data.server

import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.NetworkFailure
import com.serratocreations.phovo.core.model.network.ServerHealth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Outcome of probing a candidate address. */
sealed interface ProbeResult {
    data class Healthy(val health: ServerHealth) : ProbeResult
    data class Failed(val reason: NetworkFailure) : ProbeResult
}

/**
 * Probes a Phovo server for liveness and identity.
 *
 * Deliberately given its own [HttpClient] rather than the shared application one, whose request
 * timeout is measured in minutes because it carries photo uploads. A probe that takes minutes to
 * fail is useless for deciding whether to go looking for the server somewhere else.
 */
class ServerHealthProbe(
    private val client: HttpClient
) {
    suspend fun probe(baseUrl: BaseUrl): ProbeResult = try {
        val response: HttpResponse = client.get(baseUrl / ApiEndpoints.HEALTH_API)
        if (response.status.isSuccess()) {
            ProbeResult.Healthy(response.body())
        } else {
            ProbeResult.Failed(NetworkFailure.HttpStatus)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ProbeResult.Failed(classify(e))
    }

    private fun classify(e: Exception): NetworkFailure = when (e) {
        is io.ktor.client.plugins.HttpRequestTimeoutException,
        is io.ktor.client.network.sockets.ConnectTimeoutException,
        is io.ktor.client.network.sockets.SocketTimeoutException -> NetworkFailure.Timeout
        is kotlinx.serialization.SerializationException -> NetworkFailure.Malformed
        is kotlinx.io.IOException -> NetworkFailure.Unreachable
        else -> NetworkFailure.Unknown
    }
}
