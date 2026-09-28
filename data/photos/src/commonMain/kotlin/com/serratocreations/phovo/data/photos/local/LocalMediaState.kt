package com.serratocreations.phovo.data.photos.local

data class LocalMediaBackupProgress(
    val syncedCount: Int = 0,
    /**
     * When [isSyncComplete] then this quantity indicates items which failed to sync
     */
    val currentPendingSyncQuantity: Int = 0,
    val isSyncComplete: Boolean = false,
    val isScanningComplete: Boolean = false
) {
    val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
}