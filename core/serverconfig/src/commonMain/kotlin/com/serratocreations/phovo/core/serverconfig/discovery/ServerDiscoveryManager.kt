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
    fun startDiscovery(): Flow<List<DiscoveredServer>>
    fun stopDiscovery()
    suspend fun connectToServer(server: DiscoveredServer)
}
