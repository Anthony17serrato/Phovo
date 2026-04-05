package com.serratocreations.phovo.data.server.data.model

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
        val backupDirectory: PlatformFile
    ): ServerConfig

    data class ClientSpecificServerConfig(
        val serverBaseUrlString: String
    ): ServerConfig
}