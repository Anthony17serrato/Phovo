package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface NetworkFile {
    val mediaItem: MediaItem

    suspend fun exists(): Boolean

    suspend fun readInChunks(chunkSize: Int = DEFAULT_CHUNK_SIZE): Flow<ByteArray>

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 1024 * 1024 // 1 MB
    }
}

expect fun getNetworkFile(mediaItem: MediaItem): NetworkFile