package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton

@Singleton
class PhovoItemRepository(
    private val localPhotosDataSource: PhovoItemDao,
    private val remotePhotosDataSource: PhotosNetworkDataSource
) {
    private val eventLogs = MutableStateFlow(emptyList<String>())

    fun phovoItemsFlow() : Flow<List<PhovoItem>> =
        localPhotosDataSource.allItemsFlow().map { items ->
            syncImage(items.filterIsInstance<PhovoImageItem>())
            items.sortedByDescending {
                it.dateInFeed
            }
        }

    // TODO move to new server event repo
    fun addServerEventLog(eventLog: String) {
        eventLogs.update {
            (it + eventLog)
        }
    }

    fun serverEventLogsFlow() : Flow<List<String>> = eventLogs.asSharedFlow()

    fun syncImage(imageItem: List<PhovoImageItem>) {
        GlobalScope.launch {
            imageItem.forEach {
                remotePhotosDataSource.syncImage(it)
            }
        }
    }
}