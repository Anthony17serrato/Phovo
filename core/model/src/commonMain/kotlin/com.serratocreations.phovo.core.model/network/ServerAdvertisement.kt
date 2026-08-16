package com.serratocreations.phovo.core.model.network

/**
 * What a Phovo server publishes about itself in its mDNS TXT record, and what a client reads back.
 *
 * Both the encoder (desktop, JmDNS) and the decoders (Android NsdManager, iOS NSNetService) live on
 * opposite sides of the wire and in different source sets, so the key names and the codec are kept
 * here — in shared code — to stop the two ends from drifting apart silently.
 */
data class ServerAdvertisement(
    val serverId: String,
    val serverName: String?,
    val protocolVersion: Int,
    val scheme: String,
    /** Every address the server believes it is reachable on, in the order it prefers them. */
    val addresses: List<String>
)

object ServerTxtRecord {
    const val KEY_ID = "id"
    const val KEY_PROTOCOL_VERSION = "v"
    const val KEY_NAME = "name"
    const val KEY_SCHEME = "scheme"
    const val KEY_ADDRESSES = "addrs"

    const val SCHEME_HTTP = "http"

    private const val ADDRESS_SEPARATOR = ","

    fun encode(
        serverId: String,
        serverName: String,
        addresses: List<String>,
        scheme: String = SCHEME_HTTP,
        protocolVersion: Int = PhovoProtocol.VERSION
    ): Map<String, String> = mapOf(
        KEY_ID to serverId,
        KEY_PROTOCOL_VERSION to protocolVersion.toString(),
        KEY_NAME to serverName,
        KEY_SCHEME to scheme,
        KEY_ADDRESSES to addresses.joinToString(ADDRESS_SEPARATOR)
    )

    /**
     * @return null when the record carries no usable identity — a malformed or truncated TXT
     * record, or some other `_phovo._tcp` responder. Such a service cannot be matched to a paired
     * server and should be ignored.
     */
    fun decode(properties: Map<String, String?>): ServerAdvertisement? {
        val serverId = properties[KEY_ID]?.takeIf { it.isNotBlank() } ?: return null
        return ServerAdvertisement(
            serverId = serverId,
            serverName = properties[KEY_NAME]?.takeIf { it.isNotBlank() },
            protocolVersion = properties[KEY_PROTOCOL_VERSION]?.toIntOrNull() ?: PhovoProtocol.VERSION,
            scheme = properties[KEY_SCHEME]?.takeIf { it.isNotBlank() } ?: SCHEME_HTTP,
            addresses = properties[KEY_ADDRESSES]
                ?.split(ADDRESS_SEPARATOR)
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
        )
    }
}
