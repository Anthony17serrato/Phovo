package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.data.photos.local.model.PhovoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class WasmLocalPhotoProvider : LocalPhotoProvider {
    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        return flowOf(emptyList())
    }
}