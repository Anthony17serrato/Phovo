package com.serratocreations.phovo.core.serverconfig.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class JvmServerDiscoveryManager : ServerDiscoveryManager {
    override fun discoverServers(): Flow<List<DiscoveredServer>> = flowOf(emptyList())
    override suspend fun connectToServer(server: DiscoveredServer) {}
}
