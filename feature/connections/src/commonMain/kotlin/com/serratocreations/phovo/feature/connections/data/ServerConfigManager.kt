package com.serratocreations.phovo.feature.connections.data

import kotlinx.coroutines.flow.Flow

interface ServerConfigManager {
    fun observeDeviceServerConfigurationState(): Flow<ConfigStatus>
    fun configureDeviceAsServer()
}

sealed interface ConfigStatus {
    data object NotConfigured: ConfigStatus
    data class Configured(val serverState: ServerState): ConfigStatus
}

enum class ServerState {
    Online,
    Offline
}