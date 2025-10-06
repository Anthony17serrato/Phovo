package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.model.network.MediaItemDto
import kotlinx.coroutines.flow.Flow

interface NetworkFile {
    val mediaItemDto: MediaItemDto

    suspend fun exists(): Boolean

    suspend fun readInChunks(chunkSize: Int = DEFAULT_CHUNK_SIZE): Flow<ByteArray>

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 1024 * 1024 // 1 MB
    }
}

expect fun getNetworkFile(mediaItemDto: MediaItemDto): NetworkFile