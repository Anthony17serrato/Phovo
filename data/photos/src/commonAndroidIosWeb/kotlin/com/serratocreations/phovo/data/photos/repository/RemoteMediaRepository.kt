package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.network.model.SyncResult
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

interface RemoteMediaRepository: MediaRepository {
    suspend fun syncMedia(media: MediaItemDto, mediaUri: String): SyncResult
}

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
}