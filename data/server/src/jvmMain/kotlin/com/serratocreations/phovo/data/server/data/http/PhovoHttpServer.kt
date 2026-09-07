package com.serratocreations.phovo.data.server.data.http

import com.serratocreations.phovo.core.logger.PhovoLogger
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Owns the lifecycle of the embedded HTTP server.
 *
 * Exists because the engine handle has to be kept somewhere: configuring the device as a server is
 * a repeatable action — it runs at startup and again whenever the user reconfigures — so the
 * previous engine must be released before a port is bound again. Discarding the handle made both
 * impossible, and also hid bind failures, since the exception surfaced on an engine nobody held.
 */
class PhovoHttpServer(
    logger: PhovoLogger
) {
    private val log = logger.withTag("PhovoHttpServer")
    private var engine: EmbeddedServer<*, *>? = null

    private companion object {
        /**
         * Tried first because a predictable port is what someone typing an address by hand will
         * assume. 8080 is heavily contended on developer machines, hence the fallback.
         */
        const val PREFERRED_PORT = 8080

        /** Binding port 0 asks the OS for any free port. */
        const val EPHEMERAL_PORT = 0

        const val BIND_ALL_INTERFACES = "0.0.0.0"
        const val STOP_GRACE_MILLIS = 1_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }

    /**
     * Starts the server, replacing any instance already running.
     *
     * Falls back to an OS-assigned port when the preferred one is taken, rather than refusing to
     * start: mDNS advertises the port in its SRV record and both clients read it from there, so a
     * discovered server is reachable on whatever port it ended up with.
     *
     * @return the port actually bound, or null if the server could not be started at all.
     */
    suspend fun start(module: Application.() -> Unit): Int? {
        stop()
        return bind(PREFERRED_PORT, module)
            ?: bind(EPHEMERAL_PORT, module).also { fallbackPort ->
                if (fallbackPort != null) {
                    log.i { "Port $PREFERRED_PORT unavailable, bound $fallbackPort instead" }
                }
            }
    }

    private suspend fun bind(port: Int, module: Application.() -> Unit): Int? {
        var started: EmbeddedServer<*, *>? = null
        return try {
            started = embeddedServer(
                factory = Netty,
                port = port,
                host = BIND_ALL_INTERFACES,
                module = module
            ).also { it.start(wait = false) }

            // Binding is what actually fails, and it completes asynchronously — resolvedConnectors()
            // is where that surfaces, and is also the only place the OS-assigned port can be read.
            val boundPort = started.engine.resolvedConnectors().first().port
            engine = started
            log.i { "Phovo server listening on $BIND_ALL_INTERFACES:$boundPort" }
            boundPort
        } catch (e: Exception) {
            log.e(e) { "Could not bind port $port" }
            // A half-started engine still holds Netty event loop threads.
            runCatching { started?.stop(STOP_GRACE_MILLIS, STOP_TIMEOUT_MILLIS) }
            null
        }
    }

    /** Releases the port. Safe to call when nothing is running. */
    fun stop() {
        val running = engine ?: return
        engine = null
        try {
            running.stop(gracePeriodMillis = STOP_GRACE_MILLIS, timeoutMillis = STOP_TIMEOUT_MILLIS)
            log.i { "Phovo server stopped" }
        } catch (e: Exception) {
            log.e(e) { "Error stopping Phovo server" }
        }
    }
}
