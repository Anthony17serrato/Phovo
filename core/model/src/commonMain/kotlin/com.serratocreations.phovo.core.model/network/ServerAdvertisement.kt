package com.serratocreations.phovo.core.model.network

/**
 * What a Phovo server publishes about itself in its mDNS TXT record.
 *
 * The encoder runs on the desktop through JmDNS and the decoders run on Android through NsdManager
 * and on iOS through NSNetService, so the three live in different source sets and cannot see each
 * other. Keeping the key names and the codec here, in shared code, is what stops them drifting
 * apart without anything failing to compile.
 */
data class ServerAdvertisement(
    val serverId: String,
    val protocolVersion: Int
)

object ServerTxtRecord {
    const val KEY_ID = "id"
    const val KEY_PROTOCOL_VERSION = "v"

    fun encode(
        serverId: String,
        protocolVersion: Int = PhovoProtocol.VERSION
    ): Map<String, String> = mapOf(
        KEY_ID to serverId,
        KEY_PROTOCOL_VERSION to protocolVersion.toString()
    )

    /**
     * @return null when the record carries no usable identity, which is either a malformed record
     * or some other responder on `_phovo._tcp`. Such a service cannot be matched to a paired
     * server and should be ignored.
     */
    fun decode(properties: Map<String, String?>): ServerAdvertisement? {
        val serverId = properties[KEY_ID]?.takeIf { it.isNotBlank() } ?: return null
        return ServerAdvertisement(
            serverId = serverId,
            // A record without a version predates versioning, so assume the first.
            protocolVersion = properties[KEY_PROTOCOL_VERSION]?.toIntOrNull() ?: PhovoProtocol.VERSION
        )
    }
}
