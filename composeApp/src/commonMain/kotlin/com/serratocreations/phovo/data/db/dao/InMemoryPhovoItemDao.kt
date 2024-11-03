package com.serratocreations.phovo.data.db.dao

import com.serratocreations.phovo.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class InMemoryPhovoItemDao : PhovoItemDao {
    override fun addItem(phovoItem: PhovoItem) {
        TODO("Not yet implemented")
    }

    override fun allItemsFlow(): Flow<List<PhovoItem>> {
        return flowOf(listOf(PhovoItem(itemId = 1L, title = "Test", contents = "Some Contents")))
    }

    override fun updatePhovoItem(phovoItem: PhovoItem): Boolean {
        TODO("Not yet implemented")
    }
}