package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.database.dao.ClientConfigDao
import com.serratocreations.phovo.core.database.entities.ClientConfigEntity
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.model.network.BaseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IosAndroidServerConfigRepository(
    private val clientConfigDao: ClientConfigDao
) : ServerConfigRepository {

    override fun observeServerConfig(): Flow<ServerConfig.ClientSpecificServerConfig?> {
        return clientConfigDao.clientConfigFlow().map { entity ->
            entity?.serverUrl?.let { url ->
                ServerConfig.ClientSpecificServerConfig(
                    serverBaseUrlString = BaseUrl(url),
                    serverId = entity.serverId,
                    serverName = entity.serverName
                )
            }
        }
    }

    /**
     * Pairs this client with a server.
     *
     * @param serverUrl the address the server answered on when pairing.
     * @param serverId identity from the server's TXT record, null when pairing from a manually
     * entered address.
     * @param serverName the name advertised over mDNS, kept only as a first label to show until the
     * server reports its own name. Null when pairing from a manually entered address.
     */
    suspend fun updateClientServerConfig(
        serverUrl: String,
        serverId: String? = null,
        serverName: String? = null
    ) {
        clientConfigDao.insert(
            ClientConfigEntity(
                serverUrl = serverUrl,
                serverId = serverId,
                serverName = serverName
            )
        )
    }

    /**
     * Refreshes the cached name from a health response. The server is the only authority on what it
     * calls itself: the name pairing started with came from mDNS, which renames services on
     * collision, and the user can rename the server at any time.
     */
    suspend fun updateCachedServerNameIfNew(serverName: String) =
        clientConfigDao.updateServerName(serverName)

    /**
     * Records where the paired server was found after it moved. Identity is untouched: this is the
     * same server at a new address, which is the case the whole pairing design exists to survive.
     */
    suspend fun updateCachedServerUrl(serverUrl: String) =
        clientConfigDao.updateServerUrl(serverUrl)

    suspend fun clearClientServerConfig() = clientConfigDao.deleteConfig()
}
