package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.mapper.asEntity
import com.serratocreations.phovo.core.serverconfig.mapper.asServerSpecificServerConfig
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

    override suspend fun updateClientServerConfig(serverUrl: String) {
        // No-op for desktop server target
    }

    override suspend fun clearClientServerConfig() {
        // No-op for desktop server target
    }
}