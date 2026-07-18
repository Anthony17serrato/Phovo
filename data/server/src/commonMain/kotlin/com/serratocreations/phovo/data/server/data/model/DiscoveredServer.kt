package com.serratocreations.phovo.data.server.data.model

data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int
) {
    val url: String get() = "http://$ipAddress:$port"
}