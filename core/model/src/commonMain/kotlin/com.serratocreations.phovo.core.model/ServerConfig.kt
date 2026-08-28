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

    data class ClientSpecificServerConfig(
        val serverBaseUrlString: BaseUrl,
        // Todo make not nullable
        /**
         * Identity of the paired server. Null when pairing came from a manually entered address,
         * where nothing has yet told us who answers there.
         */
        val serverId: String? = null,
        // Todo make not nullable
        /**
         * Cached display name of the paired server, refreshed from its health responses. Null until
         * the server has told us its name — show the address in the meantime.
         */
        val serverName: String? = null
    ): ServerConfig
}