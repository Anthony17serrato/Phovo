package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.database.dao.ClientConfigDao
import com.serratocreations.phovo.core.database.entities.ClientConfigEntity
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.model.network.BaseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class IosAndroidServerConfigRepository(
    private val clientConfigDao: ClientConfigDao
) : ServerConfigRepository {

    /**
     * Emits the identity of the paired server. Address changes deliberately do not appear here —
     * see [ServerConfig.ClientSpecificServerConfig] — so downstream `flatMapLatest` operators are
     * not restarted when the server merely moves. Use [cachedServerUrl] for the address.
     */
    override fun observeServerConfig(): Flow<ServerConfig.ClientSpecificServerConfig?> {
        return clientConfigDao.clientConfigFlow().map { entity ->
            entity?.let {
                ServerConfig.ClientSpecificServerConfig(
                    serverId = it.serverId,
                    serviceName = it.serviceName
                )
            }
        // Room re-emits on any write to the table, including one that only touched the cached
        // address, so without this every re-resolution would still restart downstream flows.
        }.distinctUntilChanged()
    }

    /** The last address the server answered on, if any. */
    suspend fun cachedServerUrl(): BaseUrl? =
        clientConfigDao.getClientConfig()?.serverUrl?.let(::BaseUrl)

    /** Records a newly resolved address without disturbing the stored identity. */
    suspend fun updateCachedServerUrl(serverUrl: String) =
        clientConfigDao.updateServerUrl(serverUrl)

    /**
     * Adopts the identity a server reported, for a pairing created without one. No-op once an
     * identity is stored, so this can never silently repoint the client at a different server.
     */
    suspend fun adoptServerId(serverId: String) = clientConfigDao.adoptServerId(serverId)

    override suspend fun updateClientServerConfig(
        serverUrl: String,
        serverId: String?,
        serviceName: String?
    ) {
        clientConfigDao.insert(
            ClientConfigEntity(
                serverUrl = serverUrl,
                serverId = serverId,
                serviceName = serviceName
            )
        )
    }

    override suspend fun clearClientServerConfig() = clientConfigDao.deleteConfig()
}
