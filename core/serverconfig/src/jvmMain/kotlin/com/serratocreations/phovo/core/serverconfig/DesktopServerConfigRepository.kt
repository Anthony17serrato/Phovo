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
     * Writes the config, preserving the existing server identity if there is one. Regenerating the
     * id would make every already-paired client see a different server, so it is minted exactly
     * once — on the first configuration — and carried forward on every write after that.
     */
    suspend fun updateServerConfig(serverConfig: ServerConfig.ServerSpecificServerConfig) =
        localDataSource.insert(
            serverConfig.asEntity(serverId = serverId() ?: UUID.randomUUID().toString())
        )

    /**
     * This server's stable identity, or null if the device has not been configured as a server yet.
     * Only [updateServerConfig] mints one; this never creates, so that two callers racing before
     * configuration cannot end up with different ids.
     */
    suspend fun serverId(): String? = localDataSource.serverConfigFlow().first()?.serverId

    override fun observeServerConfig(): Flow<ServerConfig.ServerSpecificServerConfig?> = localDataSource.serverConfigFlow()
        .map { serverConfigEntity ->
            serverConfigEntity?.asServerSpecificServerConfig()
        }

    override suspend fun updateClientServerConfig(
        serverUrl: String,
        serverId: String?,
        serviceName: String?
    ) {
        // No-op for desktop server target
    }

    override suspend fun clearClientServerConfig() {
        // No-op for desktop server target
    }
}