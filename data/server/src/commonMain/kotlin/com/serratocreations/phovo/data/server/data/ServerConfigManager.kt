package com.serratocreations.phovo.data.server.data

import com.serratocreations.phovo.core.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface DesktopServerConfigManager {
    fun observeDeviceServerConfigurationState(scope: CoroutineScope): Flow<ServerConfigState>
    fun configureDeviceAsServer(serverConfig: ServerConfig.ServerSpecificServerConfig)
    fun getDefaultServerName(): String
}

sealed interface ConfigStatus {
    data object Loading: ConfigStatus
    data object NotConfigured : ConfigStatus
    data object Starting : ConfigStatus
    data class Configured(
        val serverUrl: String,
    ) : ConfigStatus
}

data class ServerConfigState(
    val configStatus: ConfigStatus = ConfigStatus.Loading,
    val serverEventLogs: List<String> = emptyList()
)