package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri
import kotlinx.coroutines.flow.Flow

class WasmNetworkFile(
    override val uri: Uri
) : NetworkFile {

    override suspend fun exists(): Boolean =
        TODO("Implement when WASM client should support uploads")

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> {
        TODO("Not yet implemented")
    }
}

actual fun getNetworkFile(uri: Uri):NetworkFile = WasmNetworkFile(uri)