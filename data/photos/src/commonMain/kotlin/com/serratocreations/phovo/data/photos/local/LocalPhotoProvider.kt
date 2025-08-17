package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.data.photos.local.model.PhovoItem
import kotlinx.coroutines.flow.Flow

// TODO: UI will consume DAO APIs instead, make this internal after DAO API is implemented
/**
 * Provides both photos and videos from the local device.
 * These items must be processed by a metadata extraction job before being displayed.
 * This data source should not be consumed directly from the UI layer
 */
interface LocalPhotoProvider {
    /**
     * Currently local directory is only used for desktop clients to fetch backed up images.
     */
    fun allItemsFlow(localDirectory: String? = null) : Flow<List<PhovoItem>>
}