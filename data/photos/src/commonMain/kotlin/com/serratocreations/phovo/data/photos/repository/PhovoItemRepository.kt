package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Singleton

@Singleton
class PhovoItemRepository(
    private val phovoItemDao: PhovoItemDao
) {
    fun phovoItemsFlow() : Flow<List<PhovoItem>> =
        phovoItemDao.allItemsFlow().map { items ->
            items.sortedByDescending { it.dateInFeed }
        }
}