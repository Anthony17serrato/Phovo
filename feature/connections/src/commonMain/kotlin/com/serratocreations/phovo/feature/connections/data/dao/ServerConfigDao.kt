package com.serratocreations.phovo.feature.connections.data.dao

import com.serratocreations.phovo.feature.connections.data.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Singleton

@Singleton
class ServerConfigDao {
    // TODO: persist to room
    private val _serverConfigInMemoryDataSource = MutableStateFlow(ServerConfig(""))
    val serverConfigInMemoryDataSource = _serverConfigInMemoryDataSource.asStateFlow()

    fun updateServerConfig(serverConfig: ServerConfig) {
        _serverConfigInMemoryDataSource.update {
            serverConfig
        }
    }
}