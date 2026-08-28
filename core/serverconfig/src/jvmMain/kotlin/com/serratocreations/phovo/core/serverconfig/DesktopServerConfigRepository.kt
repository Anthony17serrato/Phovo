package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.mapper.asEntity
import com.serratocreations.phovo.core.serverconfig.mapper.asServerSpecificServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class DesktopServerConfigRepository(
    private val localDataSource: ServerConfigDao
): ServerConfigRepository {
    /**
     * Writes the config, carrying forward the existing server identity. Regenerating the id would
     * make every already-paired client see a different server, so it is generated once, on the
     * first configuration, and reused on every write after that.
     */
    suspend fun updateServerConfig(serverConfig: ServerConfig.ServerSpecificServerConfig) =
        localDataSource.insert(
            serverConfig.asEntity(serverId = serverId() ?: UUID.randomUUID().toString())
        )

    /**
     * This server's stable identity, or null before the device has been configured as a server.
     * This only reads; [updateServerConfig] is the one place that generates an id, so two callers
     * racing before configuration cannot end up with different ones.
     */
    suspend fun serverId(): String? = localDataSource.serverConfigFlow().first()?.serverId

    override fun observeServerConfig(): Flow<ServerConfig.ServerSpecificServerConfig?> = localDataSource.serverConfigFlow()
        .map { serverConfigEntity ->
            serverConfigEntity?.asServerSpecificServerConfig()
        }
}