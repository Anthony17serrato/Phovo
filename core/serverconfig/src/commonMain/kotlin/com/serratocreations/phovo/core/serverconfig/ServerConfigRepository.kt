package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.model.ServerConfig
import kotlinx.coroutines.flow.Flow

interface ServerConfigRepository {
    fun observeServerConfig(): Flow<ServerConfig?>

    /**
     * Pairs this client with a server.
     *
     * @param serverUrl the address the server was reachable at when pairing. Stored only as a
     * starting point; [serverId] is what the connection is actually keyed on.
     * @param serverId null when pairing from a manually entered address, where no identity is
     * known until the first health probe succeeds and the client adopts what the server reports.
     * @param serviceName mDNS service instance name, used as a hint when re-browsing.
     */
    suspend fun updateClientServerConfig(
        serverUrl: String,
        serverId: String? = null,
        serviceName: String? = null
    )

    suspend fun clearClientServerConfig()
}
