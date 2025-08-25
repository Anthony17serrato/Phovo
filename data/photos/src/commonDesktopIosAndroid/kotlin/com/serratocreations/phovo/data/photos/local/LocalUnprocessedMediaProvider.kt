package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.data.photos.repository.model.PhovoItem
import kotlinx.coroutines.flow.Flow

/**
 * Provides both photos and videos from the local device.
 * These items must be processed by a metadata extraction job before being displayed.
 * This data source should not be consumed directly from the UI layer
 */
interface LocalUnprocessedMediaProvider {
    /**
     * Currently local directory is only used for desktop clients to fetch backed up images.
     */
    fun allItemsFlow(localDirectory: String? = null) : Flow<List<PhovoItem>>
}