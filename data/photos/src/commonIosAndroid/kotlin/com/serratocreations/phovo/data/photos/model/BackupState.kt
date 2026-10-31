package com.serratocreations.phovo.data.photos.model

import com.serratocreations.phovo.data.photos.repository.model.SyncState

data class BackupState(
    val isProcessing: Boolean, // TODO val processing: ProcessingState,   // e.g. Running(progress) / Done(progress)
    val syncState: SyncState
) {
    val isIdle: Boolean get() = !isProcessing && syncState !is SyncState.Syncing
}