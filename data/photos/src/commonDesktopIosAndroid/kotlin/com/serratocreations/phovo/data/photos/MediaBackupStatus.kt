package com.serratocreations.phovo.data.photos


sealed interface MediaBackupStatus

data object Scanning: MediaBackupStatus

data class MediaBackupProgress(
    val syncedCount: Int = 0,
    private val currentPendingSyncQuantity: Int = 0,
    val isSyncComplete: Boolean = false
): MediaBackupStatus {
    val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
}

data class BackupComplete(
    val backedUpQuantity: Int,
    val failureQuantity: Int
): MediaBackupStatus