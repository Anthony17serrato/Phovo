package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri

class WasmNetworkFile(
    override val uri: Uri
) : NetworkFile {

    override suspend fun exists(): Boolean =
        TODO("Implement when WASM client should support uploads")

    override suspend fun readBytes() =
        TODO("Implement when WASM client should support uploads")
}

actual fun getNetworkFile(uri: Uri):NetworkFile = WasmNetworkFile(uri)