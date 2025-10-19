package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.model.network.MediaItemDto
import kotlinx.coroutines.flow.Flow

interface NetworkFile {
    val mediaItemDto: MediaItemDto
    val uri: String

    suspend fun exists(): Boolean

    suspend fun readInChunks(chunkSize: Int = DEFAULT_CHUNK_SIZE): Flow<ByteArray>

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 256 * 1024 // 256 kb
    }
}

expect fun getNetworkFile(mediaItemDto: MediaItemDto, uri: String): NetworkFile