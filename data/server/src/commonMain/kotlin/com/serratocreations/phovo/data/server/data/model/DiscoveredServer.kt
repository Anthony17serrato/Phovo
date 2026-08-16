package com.serratocreations.phovo.data.server.data.model

data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int,
    /**
     * Identity advertised in the service's TXT record, or null if the record could not be read.
     * Used to recognise a known server that has moved to a new address.
     */
    val serverId: String? = null,
    val scheme: String = "http",
    /**
     * Every address the server advertised, most preferred first. [ipAddress] is the one that
     * resolution produced; the rest are fallbacks for a multi-homed host.
     */
    val alternateAddresses: List<String> = emptyList()
) {
    val url: String get() = buildUrl(ipAddress)

    /** Addresses to try in order, starting with the resolved one. */
    val candidateUrls: List<String>
        get() = (listOf(ipAddress) + alternateAddresses).distinct().map(::buildUrl)

    private fun buildUrl(host: String): String {
        // A bare IPv6 literal has to be bracketed before it can go in a URL authority.
        val authority = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        return "$scheme://$authority:$port"
    }
}
