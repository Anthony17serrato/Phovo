package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Singleton

@Singleton
class PhovoItemRepository(
    private val phovoItemDao: PhovoItemDao
) {
    private val eventLogs = MutableStateFlow(emptyList<String>())

    fun phovoItemsFlow() : Flow<List<PhovoItem>> =
        phovoItemDao.allItemsFlow().map { items ->
            items.sortedByDescending { it.dateInFeed }
        }

    // TODO move to new server event repo
    fun addServerEventLog(eventLog: String) {
        eventLogs.update {
            (it + eventLog)
        }
    }

    fun serverEventLogsFlow() : Flow<List<String>> = eventLogs.asSharedFlow()
}