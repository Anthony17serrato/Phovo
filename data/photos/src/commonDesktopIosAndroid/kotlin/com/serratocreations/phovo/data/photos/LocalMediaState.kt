package com.serratocreations.phovo.data.photos


sealed interface LocalMediaState

data object Scanning: LocalMediaState

data class LocalMediaBackupProgress(
    val syncedCount: Int = 0,
    val currentPendingSyncQuantity: Int = 0,
    val isSyncComplete: Boolean = false
): LocalMediaState {
    val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
}

data class BackupCompleteLocal(
    val backedUpQuantity: Int,
    val failureQuantity: Int
): LocalMediaState