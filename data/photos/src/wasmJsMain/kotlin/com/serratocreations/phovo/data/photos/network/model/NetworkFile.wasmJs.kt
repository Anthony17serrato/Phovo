package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.model.network.MediaItemDto
import kotlinx.coroutines.flow.Flow

class WasmNetworkFile(
    override val mediaItemDto: MediaItemDto,
    override val uri: String
) : NetworkFile {

    override suspend fun exists(): Boolean =
        TODO("Implement when WASM client should support uploads")

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> {
        TODO("Not yet implemented")
    }
}

actual fun getNetworkFile(mediaItemDto: MediaItemDto, uri: String):NetworkFile = WasmNetworkFile(mediaItemDto, uri)