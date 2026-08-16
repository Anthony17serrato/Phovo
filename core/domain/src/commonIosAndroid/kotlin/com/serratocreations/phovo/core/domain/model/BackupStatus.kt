package com.serratocreations.phovo.core.domain.model

import com.serratocreations.phovo.core.model.network.NetworkFailure

sealed interface BackupStatus {
    /**
     * @param reason why the server could not be reached, or null when no server is configured yet.
     */
    data class ServerOffline(val reason: NetworkFailure?): BackupStatus

    data object Scanning: BackupStatus

    data class LocalMediaBackupProgress(
        val syncedCount: Int = 0,
        private val currentPendingSyncQuantity: Int = 0,
        val isSyncComplete: Boolean = false
    ): BackupStatus {
        val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
    }

    data class BackupCompleteLocal(
        val backedUpQuantity: Int,
        val failureQuantity: Int
    ): BackupStatus
}