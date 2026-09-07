package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.model.network.isConnected
import kotlinx.coroutines.flow.Flow

interface RemoteMediaRepository: MediaRepository {
    suspend fun syncMedia(media: MediaItemDto, mediaUri: String): NetworkResult<Unit>

    /**
     * Observes the connection to the phovo server by periodically pinging the server endpoint.
     * Callers that only need to gate work on connectivity can reduce this with [isConnected].
     */
    fun observeConnectionState(): Flow<ServerConnectionState>
}