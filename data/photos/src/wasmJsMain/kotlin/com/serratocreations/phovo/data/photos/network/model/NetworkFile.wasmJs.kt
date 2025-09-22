package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow

class WasmNetworkFile(
    override val mediaItem: MediaItem
) : NetworkFile {

    override suspend fun exists(): Boolean =
        TODO("Implement when WASM client should support uploads")

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> {
        TODO("Not yet implemented")
    }
}

actual fun getNetworkFile(mediaItem: MediaItem):NetworkFile = WasmNetworkFile(mediaItem)