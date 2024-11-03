package com.serratocreations.phovo.data.db.dao

import com.serratocreations.phovo.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow

interface PhovoItemDao {
    fun addItem(phovoItem: PhovoItem)

    fun allItemsFlow() : Flow<List<PhovoItem>>

    fun updatePhovoItem(phovoItem: PhovoItem) : Boolean
}