package com.serratocreations.phovo.feature.connections.data

import kotlinx.coroutines.flow.Flow

interface ServerConfigManager {
    fun observeDeviceServerConfigurationState(): Flow<ConfigStatus>
}

enum class ConfigStatus {
    NotConfigured,
    Online,
    Offline
}