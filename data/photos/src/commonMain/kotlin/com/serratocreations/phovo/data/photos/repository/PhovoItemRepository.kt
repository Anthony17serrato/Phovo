package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.di.ApplicationScope
import com.serratocreations.phovo.core.common.getPlatform
import com.serratocreations.phovo.data.photos.local.LocalPhotoProvider
import com.serratocreations.phovo.data.photos.local.model.PhovoImageItem
import com.serratocreations.phovo.data.photos.local.model.PhovoItem
import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton

@Singleton
class PhovoItemRepository(
    private val localPhotosDataSource: LocalPhotoProvider,
    private val remotePhotosDataSource: PhotosNetworkDataSource,
    @param:ApplicationScope private val appScope: CoroutineScope
) {
    fun phovoItemsFlow(localDirectory: String? = null) : Flow<List<PhovoItem>> {
        return localPhotosDataSource.allItemsFlow(localDirectory).map { items ->
            if (getPlatform() != Platform.Desktop) syncImage(items.filterIsInstance<PhovoImageItem>())
            items.sortedByDescending {
                it.dateInFeed
            }
        }
    }

    fun syncImage(imageItem: List<PhovoImageItem>) {
        appScope.launch {
            imageItem.forEach {
                remotePhotosDataSource.syncImage(it)
            }
        }
    }
}