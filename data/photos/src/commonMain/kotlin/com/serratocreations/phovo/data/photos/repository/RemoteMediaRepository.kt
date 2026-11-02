package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.model.network.isConnected
import kotlinx.coroutines.flow.Flow

interface RemoteMediaRepository: MediaRepository {
    /**
     * @param onBytesSent called as the upload body is written, with the running total of bytes sent
     * for this attempt. It restarts from zero if the upload is retried.
     */
    suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String,
        onBytesSent: (bytesSent: Long) -> Unit = {}
    ): NetworkResult<Unit>

    /**
     * Emits [ServerConnectionState.Checking], then the result of a health probe started when
     * collection starts, then every change after that. A new collector never gets a result left
     * over from the last heartbeat, which can be up to [CHECK_ALIVE_DELAY] old, so the first state
     * after [ServerConnectionState.Checking] is the server's current one.
     *
     * Distinct until changed, so a state that persists is emitted once rather than on every
     * heartbeat.
     */
    fun observeConnectionState(): Flow<ServerConnectionState>
}