package com.serratocreations.phovo.data.photos.model

import com.serratocreations.phovo.data.photos.repository.model.SyncState

sealed interface BackupState {
    /** Nothing is being processed or uploaded */
    val isIdle: Boolean

    /**
     * Phovo can't read the gallery, so nothing is processed or backed up until the user grants
     * access. Idle, so anything waiting for a backup to finish stops waiting.
     */
    data object MediaAccessRequired : BackupState {
        override val isIdle: Boolean = true
    }

    data class Active(
        val isProcessing: Boolean, // TODO val processing: ProcessingState,   // e.g. Running(progress) / Done(progress)
        val syncState: SyncState
    ) : BackupState {
        override val isIdle: Boolean get() = !isProcessing && syncState !is SyncState.Syncing
    }
}
