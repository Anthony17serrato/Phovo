package com.serratocreations.phovo.data.server.data.dao

import com.serratocreations.phovo.data.server.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Singleton

@Singleton
class ServerConfigDao {
    // TODO: persist to room
    private val _serverConfigInMemoryDataSource = MutableStateFlow(ServerConfig(null))

    fun updateServerConfig(serverConfig: ServerConfig) {
        _serverConfigInMemoryDataSource.update {
            serverConfig
        }
    }

    fun observeServerConfig(): Flow<ServerConfig> {
        return _serverConfigInMemoryDataSource.asStateFlow()
    }
}