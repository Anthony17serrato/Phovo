package com.serratocreations.phovo.core.serverconfig.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class JvmServerDiscoveryManager : ServerDiscoveryManager {
    override fun startDiscovery(): Flow<List<DiscoveredServer>> = flowOf(emptyList())
    override fun stopDiscovery() {}
    override suspend fun connectToServer(server: DiscoveredServer) {}
}
