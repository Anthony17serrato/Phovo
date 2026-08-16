package com.serratocreations.phovo.core.model

import com.serratocreations.phovo.core.model.network.BaseUrl
import io.github.vinceglb.filekit.PlatformFile

/**
 * Data model for defining information about the server which can be used
 * by both the client and server. Interface properties are information that is commonly
 * used by both the client and the server. Additionally [ServerSpecificServerConfig] defines additional
 * properties that are used only by the server, while [ClientSpecificServerConfig] defines additional
 * properties about the server that are only used by the client.
 *
 */
sealed interface ServerConfig {

    data class ServerSpecificServerConfig(
        val backupDirectory: PlatformFile,
        val serverName: String
    ): ServerConfig

    /**
     * Identifies which server this client is paired with. Deliberately carries no address: an
     * address is a lease that changes, and everything downstream of the config flow uses
     * `flatMapLatest`, so including it here would tear down and restart the health poll and the
     * media flow every time the server moved — blanking the photo feed on exactly the event this
     * design exists to absorb. The current address is owned by the endpoint resolver instead.
     *
     * @param serverId null until a manually entered pairing completes its first health probe.
     */
    data class ClientSpecificServerConfig(
        val serverId: String?,
        val serviceName: String?
    ): ServerConfig
}