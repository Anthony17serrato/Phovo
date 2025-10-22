package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.model.network.MediaItemDto

sealed interface SyncResult

data class SyncSuccessful(val updatedMediaItemDto: MediaItemDto): SyncResult

// TODO Better error modeling
data object SyncError: SyncResult