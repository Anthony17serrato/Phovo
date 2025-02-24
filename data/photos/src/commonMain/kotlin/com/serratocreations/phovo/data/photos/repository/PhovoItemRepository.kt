package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.getPlatform
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
    fun phovoItemsFlow(localDirectory: String? = null) : Flow<List<PhovoItem>> =
        localPhotosDataSource.allItemsFlow(localDirectory).map { items ->
            if (getPlatform() != Platform.Desktop) syncImage(items.filterIsInstance<PhovoImageItem>())
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