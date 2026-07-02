package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.database.dao.ClientConfigDao
import com.serratocreations.phovo.core.database.entities.ClientConfigEntity
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.model.network.BaseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class IosAndroidServerConfigRepository(
    private val clientConfigDao: ClientConfigDao
) : ServerConfigRepository {

    override fun observeServerConfig(): Flow<ServerConfig.ClientSpecificServerConfig?> {
        return clientConfigDao.clientConfigFlow().map { entity ->
            entity?.serverUrl?.let { url ->
                ServerConfig.ClientSpecificServerConfig(
                    serverBaseUrlString = BaseUrl(url)
                )
            }
        }
    }

    override suspend fun updateClientServerConfig(serverUrl: String) {
        clientConfigDao.insert(ClientConfigEntity(serverUrl = serverUrl))
    }

    override suspend fun clearClientServerConfig() {
        clientConfigDao.deleteConfig()
    }
}
