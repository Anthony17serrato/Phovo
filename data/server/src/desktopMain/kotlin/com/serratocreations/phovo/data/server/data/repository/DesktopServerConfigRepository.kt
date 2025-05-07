package com.serratocreations.phovo.data.server.data.repository

import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.data.server.data.mapper.asEntity
import com.serratocreations.phovo.data.server.data.mapper.asExternalModel
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory(binds = [DesktopServerConfigRepository::class, ServerConfigRepository::class])
class DesktopServerConfigRepository(
    private val localDataSource: ServerConfigDao
): ServerConfigRepository() {
    suspend fun updateServerConfig(serverConfig: ServerConfig) = localDataSource.insert(serverConfig.asEntity())

    override fun observeServerConfig() = localDataSource.serverConfigFlow()
        .map { serverConfigEntity ->
            serverConfigEntity?.asExternalModel()
        }
}