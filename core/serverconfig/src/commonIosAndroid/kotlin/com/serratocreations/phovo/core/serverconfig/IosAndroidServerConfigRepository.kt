package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.model.network.BaseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosAndroidServerConfigRepository: ServerConfigRepository {
    override fun observeServerConfig(): Flow<ServerConfig.ClientSpecificServerConfig?> {
        // TODO add logic for setting and reading config from DB
        return flowOf(
            ServerConfig.ClientSpecificServerConfig(
                serverBaseUrlString = BaseUrl("http://10.0.0.231:8080/")
            )
        )
    }
}