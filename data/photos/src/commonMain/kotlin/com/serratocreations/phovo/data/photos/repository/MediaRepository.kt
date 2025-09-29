package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

open class MediaRepository(
    private val remotePhotosDataSource: MediaNetworkDataSource
) {
    open fun phovoMediaFlow() : Flow<List<MediaItem>> {
        return remotePhotosDataSource.allItemsFlow().onStart { emit(emptyList()) }
    }
}