package com.serratocreations.phovo.data.photos.db.dao

import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DesktopPhovoItemDao : PhovoItemDao {

    override fun allItemsFlow(): Flow<List<PhovoItem>> {
        return flowOf(emptyList())
    }
}