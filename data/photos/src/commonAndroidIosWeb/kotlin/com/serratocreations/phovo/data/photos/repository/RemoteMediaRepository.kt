package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

class RemoteMediaRepositoryImpl(
    private val remotePhotosDataSource: MediaNetworkDataSource
): RemoteMediaRepository {
    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        return remotePhotosDataSource.allItemsFlow().onStart { emit(emptyList()) }
    }

    override suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String
    ): SyncResult {
        return remotePhotosDataSource.syncMedia(
            mediaItemDto = media,
            mediaUri = mediaUri
        )
    }

    override suspend fun observeServerConnection(): Flow<Boolean> = flow {
        while(currentCoroutineContext().isActive) {
            yield()
            emit(remotePhotosDataSource.checkServerConnection())
            delay(30.seconds)
        }
    }
}