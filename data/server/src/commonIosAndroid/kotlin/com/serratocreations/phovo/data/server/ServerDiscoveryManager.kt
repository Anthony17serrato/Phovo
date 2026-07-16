package com.serratocreations.phovo.data.server

import kotlinx.coroutines.flow.Flow

interface ServerDiscoveryManager {
    /**
     * Exposes a flow of servers which have been discovered, while
     * there is consumers of this API the flow will continue to scan for servers.
     */
    fun discoverServers(): Flow<List<DiscoveredServer>>
    suspend fun connectToServer(server: DiscoveredServer)
}
