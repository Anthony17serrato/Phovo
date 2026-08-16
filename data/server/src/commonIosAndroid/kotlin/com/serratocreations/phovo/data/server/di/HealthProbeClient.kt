package com.serratocreations.phovo.data.server.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * A client dedicated to health probes.
 *
 * Intentionally not the shared application client: that one allows minutes per request because it
 * carries photo uploads, and a probe that takes minutes to fail cannot drive a decision about
 * whether to go looking for the server elsewhere. Engine selection is left to the platform, which
 * resolves to OkHttp on Android and Darwin on iOS.
 */
internal fun createHealthProbeClient(): HttpClient = HttpClient {
    expectSuccess = false
    install(ContentNegotiation) {
        // Tolerate fields added by a newer server rather than failing the probe outright.
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = PROBE_REQUEST_TIMEOUT.inWholeMilliseconds
        connectTimeoutMillis = PROBE_CONNECT_TIMEOUT.inWholeMilliseconds
        socketTimeoutMillis = PROBE_REQUEST_TIMEOUT.inWholeMilliseconds
    }
}

private val PROBE_CONNECT_TIMEOUT = 2.seconds
private val PROBE_REQUEST_TIMEOUT = 3.seconds
