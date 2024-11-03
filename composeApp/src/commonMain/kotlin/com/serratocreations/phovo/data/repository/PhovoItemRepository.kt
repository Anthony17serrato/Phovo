package com.serratocreations.phovo.data.repository

import com.serratocreations.phovo.data.db.dao.InMemoryPhovoItemDao
import com.serratocreations.phovo.data.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow

object PhovoItemRepository {
    private val phovoItemDao: PhovoItemDao = InMemoryPhovoItemDao()

    fun addItem(phovoItem: PhovoItem) =
        phovoItemDao.addItem(phovoItem)

    fun phovoItemsFlow() : Flow<List<PhovoItem>> =
        phovoItemDao.allItemsFlow()
}