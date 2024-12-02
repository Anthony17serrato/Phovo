package com.serratocreations.phovo.feature.photos.data.db.dao

import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class WasmPhovoItemDao : PhovoItemDao {
    override fun allItemsFlow(): Flow<List<PhovoItem>> {
        return flowOf(emptyList())
    }
}