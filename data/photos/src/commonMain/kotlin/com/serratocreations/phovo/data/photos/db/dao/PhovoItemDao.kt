package com.serratocreations.phovo.data.photos.db.dao

import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow

interface PhovoItemDao {
    fun allItemsFlow() : Flow<List<PhovoItem>>
}