package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PhovoItemRepository(
    private val localPhotosDataSource: PhovoItemDao,
    private val remotePhotosDataSource: PhotosNetworkDataSource,
    private val appScope: CoroutineScope
) {
    fun phovoItemsFlow() : Flow<List<PhovoItem>> =
        localPhotosDataSource.allItemsFlow().map { items ->
            syncImage(items.filterIsInstance<PhovoImageItem>())
            items.sortedByDescending {
                it.dateInFeed
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