package com.serratocreations.phovo.data.photos.db.dao

import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Singleton

@Singleton
class WasmPhovoItemDao : PhovoItemDao {
    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        return flowOf(emptyList())
    }
}