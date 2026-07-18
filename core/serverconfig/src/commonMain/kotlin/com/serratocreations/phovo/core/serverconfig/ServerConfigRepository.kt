package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.model.ServerConfig
import kotlinx.coroutines.flow.Flow

interface ServerConfigRepository {
    fun observeServerConfig(): Flow<ServerConfig?>
    suspend fun updateClientServerConfig(serverUrl: String)
    suspend fun clearClientServerConfig()
}