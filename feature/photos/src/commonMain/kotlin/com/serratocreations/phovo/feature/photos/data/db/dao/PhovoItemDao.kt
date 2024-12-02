package com.serratocreations.phovo.feature.photos.data.db.dao

import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow

interface PhovoItemDao {
    fun allItemsFlow() : Flow<List<PhovoItem>>
}