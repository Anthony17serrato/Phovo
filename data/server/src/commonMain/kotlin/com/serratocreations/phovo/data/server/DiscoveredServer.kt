package com.serratocreations.phovo.data.server

// TODO Move to CommonIOSAndroid
data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int
) {
    val url: String get() = "http://$ipAddress:$port"
}