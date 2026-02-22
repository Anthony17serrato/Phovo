package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
import kotlinx.coroutines.flow.Flow

interface RemoteMediaRepository: MediaRepository {
    suspend fun syncMedia(media: MediaItemDto, mediaUri: String): SyncResult

    /**
     * Observes the status of the connection to the phovo server by periodically
     * pinging the server endpoint
     */
    fun observeServerConnection(): Flow<Boolean>
}