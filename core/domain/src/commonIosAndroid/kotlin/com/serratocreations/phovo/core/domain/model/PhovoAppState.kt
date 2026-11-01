package com.serratocreations.phovo.core.domain.model

sealed interface PhovoAppState {
    /**
     * Media scanning is not mutually exclusive to any app state therefor the status is included in all
     */
    val scanningStatus: Boolean // TODO in the future this may contain more details hence the non-semantic name

    /**
     * Phovo can't read the gallery, so nothing is processed or backed up whatever the server is
     * doing.
     */
    data object MediaAccessRequired: PhovoAppState {
        override val scanningStatus: Boolean = false
    }

    data class CheckingServerConnection(
        override val scanningStatus: Boolean
    ): PhovoAppState

    data class Unconfigured(
        override val scanningStatus: Boolean
    ): PhovoAppState

    data class ServerOffline(
        override val scanningStatus: Boolean
    ): PhovoAppState

    data object SyncAwaitingItemsFromProcessingJob: PhovoAppState {
        override val scanningStatus: Boolean = true
    }

    data class LocalMediaBackupProgress(
        val syncedCount: Int = 0,
        private val currentPendingSyncQuantity: Int = 0,
        override val scanningStatus: Boolean
    ): PhovoAppState {
        val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
    }

    data class BackupCompleteLocal(
        val backedUpQuantity: Int,
        val failureQuantity: Int
    ): PhovoAppState {
        override val scanningStatus: Boolean = false
    }
}