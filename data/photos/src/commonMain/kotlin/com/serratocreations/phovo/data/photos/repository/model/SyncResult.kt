package com.serratocreations.phovo.data.photos.repository.model


sealed interface SyncResult {
    data object SyncSuccessful: SyncResult

    // TODO Better error modeling using rich errors
    data class SyncError(
        val message: String? = null
    ): SyncResult
}