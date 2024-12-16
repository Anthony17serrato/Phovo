package com.serratocreations.phovo.feature.connections.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DesktopServerConfigManager: ServerConfigManager {
    override fun observeDeviceServerConfigurationState(): Flow<ConfigStatus> {
        return flowOf(ConfigStatus.NotConfigured)
    }
}