package com.serratocreations.phovo.data.server.data.model

data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int,
    /**
     * Identity from the service's TXT record. Lets a client recognise a server it has already
     * paired with after the address changes, so a service whose record cannot be read is never
     * turned into one of these — it is dropped during discovery instead.
     */
    val serverId: String
) {
    val url: String get() = "http://$ipAddress:$port"
}