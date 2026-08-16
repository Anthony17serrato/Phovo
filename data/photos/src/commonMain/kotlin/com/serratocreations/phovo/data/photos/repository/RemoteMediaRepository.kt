package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import kotlinx.coroutines.flow.Flow

interface RemoteMediaRepository: MediaRepository {
    suspend fun syncMedia(media: MediaItemDto, mediaUri: String): NetworkResult<Unit>

    /**
     * Observes the connection to the phovo server by periodically pinging the server endpoint.
     * Prefer this over [observeServerConnection] when the reason for a failure matters, or when
     * "not configured" needs to be told apart from "configured but down".
     */
    fun observeConnectionState(): Flow<ServerConnectionState>

    /**
     * Observes whether requests to the server can currently succeed. A convenience view of
     * [observeConnectionState] for callers that only need to gate work on connectivity.
     */
    fun observeServerConnection(): Flow<Boolean>
}