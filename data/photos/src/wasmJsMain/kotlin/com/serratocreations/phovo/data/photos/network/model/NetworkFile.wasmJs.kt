package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri

class WasmNetworkFile(
    override val uri: Uri,
    private val fileName: String
) : NetworkFile {

    override suspend fun fileName(): String = fileName

    // TODO: Pending implementation
    override suspend fun exists(): Boolean = false

    override suspend fun readBytes() = null
}

actual fun getNetworkFile(uri: Uri, name: String):NetworkFile = WasmNetworkFile(uri, name)