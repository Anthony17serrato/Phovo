package com.serratocreations.phovo.core.serverconfig.discovery

import kotlinx.coroutines.flow.Flow

data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int
) {
    val url: String get() = "http://$ipAddress:$port"
}

interface ServerDiscoveryManager {
    /**
     * Exposes a flow of servers which have been discovered, while
     * there is consumers of this API the flow will continue to scan for servers.
     */
    fun discoverServers(): Flow<List<DiscoveredServer>>
    suspend fun connectToServer(server: DiscoveredServer)
}
