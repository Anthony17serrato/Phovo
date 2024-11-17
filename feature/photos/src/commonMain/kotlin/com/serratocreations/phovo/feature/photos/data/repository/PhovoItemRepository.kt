package com.serratocreations.phovo.feature.photos.data.repository

import com.serratocreations.phovo.feature.photos.data.db.dao.PhovoItemDao
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
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