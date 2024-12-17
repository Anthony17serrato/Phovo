package com.serratocreations.phovo.feature.connections.data

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DesktopServerConfigManager: ServerConfigManager {
    // Caches the current config state for new subscribers
    private val serverState = MutableSharedFlow<ConfigStatus>(replay = 1)

    init {
        // TODO: fetch the initial server state from room
        serverState.tryEmit(ConfigStatus.NotConfigured)
    }

    override fun observeDeviceServerConfigurationState(): Flow<ConfigStatus> {
        return serverState.asSharedFlow()
    }

    // TODO: Configure and use application scope
    override fun configureDeviceAsServer() {
        GlobalScope.launch {
            serverState.emit(ConfigStatus.NotConfigured)
            launch {
                embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
                    .start(wait = false)
                serverState.emit(ConfigStatus.Configured(ServerState.Online))
            }
            // TODO Save server config to room db
        }
    }
}

fun Application.module() {
    configureRouting()
}