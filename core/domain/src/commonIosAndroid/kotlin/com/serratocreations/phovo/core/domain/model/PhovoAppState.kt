package com.serratocreations.phovo.core.domain.model

import com.serratocreations.phovo.data.photos.model.ProcessingState

sealed interface PhovoAppState {
    /**
     * A state processing can run alongside, which says how far processing has got. Processing
     * isn't mutually exclusive with the server states, so they all carry it.
     */
    sealed interface WithProcessing : PhovoAppState {
        val processing: ProcessingState
    }

    /**
     * Phovo can't read the gallery, so nothing is processed or backed up whatever the server is
     * doing.
     */
    data object MediaAccessRequired: PhovoAppState

    data class CheckingServerConnection(
        override val processing: ProcessingState
    ): WithProcessing

    data class Unconfigured(
        override val processing: ProcessingState
    ): WithProcessing

    data class ServerOffline(
        override val processing: ProcessingState
    ): WithProcessing

    data class SyncAwaitingItemsFromProcessingJob(
        override val processing: ProcessingState.Running
    ): WithProcessing

    data class LocalMediaBackupProgress(
        val syncedCount: Int = 0,
        private val currentPendingSyncQuantity: Int = 0,
        override val processing: ProcessingState
    ): WithProcessing {
        val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
    }

    /** Sync only completes once processing is done, so this carries no processing state. */
    data class BackupCompleteLocal(
        val backedUpQuantity: Int,
        val failureQuantity: Int
    ): PhovoAppState
}
