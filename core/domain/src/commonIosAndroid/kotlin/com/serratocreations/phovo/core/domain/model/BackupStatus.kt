package com.serratocreations.phovo.core.domain.model

sealed interface BackupStatus {
    data object ServerOffline: BackupStatus

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