package com.serratocreations.phovo.data.repository

import com.serratocreations.phovo.data.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Singleton

@Singleton
class PhovoItemRepository(
    private val androidPhovoItemDao: PhovoItemDao
) {

    fun addItem(phovoItem: PhovoItem) =
        androidPhovoItemDao.addItem(phovoItem)

    fun phovoItemsFlow() : Flow<List<PhovoItem>> =
        androidPhovoItemDao.allItemsFlow()
}