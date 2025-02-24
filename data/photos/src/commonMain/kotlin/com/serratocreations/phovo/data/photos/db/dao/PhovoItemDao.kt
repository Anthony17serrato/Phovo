package com.serratocreations.phovo.data.photos.db.dao

import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow

interface PhovoItemDao {
    /**
     * Currently local directory is only used for desktop clients to fetch backed up images.
     */
    fun allItemsFlow(localDirectory: String? = null) : Flow<List<PhovoItem>>
}