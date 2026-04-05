package com.serratocreations.phovo.data.server.data.repository

import com.serratocreations.phovo.data.server.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosAndroidWasmServerConfigRepository: ServerConfigRepository {
    override fun observeServerConfig(): Flow<ServerConfig.ClientSpecificServerConfig?> {
        // TODO add logic for setting and reading config from DB
        return flowOf(
            ServerConfig.ClientSpecificServerConfig(
                serverBaseUrlString = "http://10.0.0.231:8080/"
            )
        )
    }
}

interface ServerConfigRepository {
    fun observeServerConfig(): Flow<ServerConfig?>
}