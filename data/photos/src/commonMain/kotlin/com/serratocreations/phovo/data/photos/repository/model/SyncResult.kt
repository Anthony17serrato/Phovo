package com.serratocreations.phovo.data.photos.repository.model

import com.serratocreations.phovo.core.model.network.MediaItemDto

sealed interface SyncResult

data class SyncSuccessful(val updatedMediaItemDto: MediaItemDto): SyncResult

// TODO Better error modeling using rich errors
data object SyncError: SyncResult