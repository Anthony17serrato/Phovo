package com.serratocreations.phovo.feature.photos.data.db.dao

import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow

interface PhovoItemDao {
    fun addItem(phovoItem: PhovoItem)

    fun allItemsFlow() : Flow<List<PhovoItem>>

    fun updatePhovoItem(phovoItem: PhovoItem) : Boolean
}