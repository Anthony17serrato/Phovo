package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.repository.model.PhovoImageItem
import com.serratocreations.phovo.data.photos.repository.model.PhovoItem
import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

open class PhovoItemRepository(
    //private val localPhotosDataSource: LocalPhotoProvider,
    private val remotePhotosDataSource: PhotosNetworkDataSource,
    private val appScope: CoroutineScope
) {
    open fun phovoItemsFlow(localDirectory: String? = null) : Flow<List<PhovoItem>> {
        return remotePhotosDataSource.allItemsFlow()

//        localPhotosDataSource.allItemsFlow(localDirectory).map { items ->
//            if (getPlatform() != Platform.Desktop) syncImage(items.filterIsInstance<PhovoImageItem>())
//            items.sortedByDescending {
//                it.dateInFeed
//            }
//        }
    }

    fun syncImage(imageItem: List<PhovoImageItem>) {
        appScope.launch {
            imageItem.forEach {
                remotePhotosDataSource.syncImage(it)
            }
        }
    }
}