package com.serratocreations.phovo.data.server.data.repository

import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.data.server.data.mapper.asEntity
import com.serratocreations.phovo.data.server.data.mapper.asServerSpecificServerConfig
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DesktopServerConfigRepository(
    private val localDataSource: ServerConfigDao
): ServerConfigRepository {
    suspend fun updateServerConfig(serverConfig: ServerConfig.ServerSpecificServerConfig) =
        localDataSource.insert(serverConfig.asEntity())

    override fun observeServerConfig(): Flow<ServerConfig.ServerSpecificServerConfig?> = localDataSource.serverConfigFlow()
        .map { serverConfigEntity ->
            serverConfigEntity?.asServerSpecificServerConfig()
        }
}