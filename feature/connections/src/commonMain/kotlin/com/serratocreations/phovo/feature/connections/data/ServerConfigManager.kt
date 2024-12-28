package com.serratocreations.phovo.feature.connections.data

import kotlinx.coroutines.flow.Flow

interface ServerConfigManager

interface DesktopServerConfigManager: ServerConfigManager {
    fun observeDeviceServerConfigurationState(): Flow<ConfigStatus>
    fun configureDeviceAsServer()
}

/**
 * Non operational server config manager for non-desktop environments
 * Currently only desktop clients support being configured as a server
 */
class NoOpServerConfigManager: ServerConfigManager

sealed interface ConfigStatus {
    data object NotConfigured: ConfigStatus
    data class Configured(val serverState: ServerState): ConfigStatus
}

enum class ServerState {
    Online,
    Offline
}