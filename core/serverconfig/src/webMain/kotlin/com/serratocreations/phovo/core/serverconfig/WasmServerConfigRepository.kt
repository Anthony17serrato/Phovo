package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.model.network.BaseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WasmServerConfigRepository : ServerConfigRepository {
    private val _serverConfig = MutableStateFlow<ServerConfig.ClientSpecificServerConfig?>(null)

    override fun observeServerConfig(): Flow<ServerConfig.ClientSpecificServerConfig?> {
        return _serverConfig.asStateFlow()
    }

    override suspend fun updateClientServerConfig(serverUrl: String) {
        _serverConfig.value = ServerConfig.ClientSpecificServerConfig(
            serverBaseUrlString = BaseUrl(serverUrl)
        )
    }

    override suspend fun clearClientServerConfig() {
        _serverConfig.value = null
    }
}
