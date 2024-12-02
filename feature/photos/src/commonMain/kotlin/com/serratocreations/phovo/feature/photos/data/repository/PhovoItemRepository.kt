package com.serratocreations.phovo.feature.photos.data.repository

import com.serratocreations.phovo.feature.photos.data.db.dao.PhovoItemDao
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
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