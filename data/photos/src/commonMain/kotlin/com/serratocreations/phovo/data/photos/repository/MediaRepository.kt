package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

open class MediaRepository(
    //private val localPhotosDataSource: LocalPhotoProvider,
    private val remotePhotosDataSource: MediaNetworkDataSource,
    private val appScope: CoroutineScope
) {
    open fun phovoMediaFlow() : Flow<List<MediaItem>> {
        return remotePhotosDataSource.allItemsFlow().onStart { emit(emptyList()) }

//        localPhotosDataSource.allItemsFlow(localDirectory).map { items ->
//            if (getPlatform() != Platform.Desktop) syncImage(items.filterIsInstance<PhovoImageItem>())
//            items.sortedByDescending {
//                it.dateInFeed
//            }
//        }
    }

//    fun syncImage(imageItem: List<PhovoImageItem>) {
//        appScope.launch {
//            imageItem.forEach {
//                remotePhotosDataSource.syncImage(it)
//            }
//        }
//    }
}