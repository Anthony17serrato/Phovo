package com.serratocreations.phovo.data.photos.repository.model

data class LocalMediaBackupProgress(
    val syncedCount: Int = 0,
    /**
     * When [isSyncComplete] then this quantity indicates items which failed to sync
     */
    val currentPendingSyncQuantity: Int,
    val isSyncComplete: Boolean = false,
    val isScanningComplete: Boolean = false,
    internal val unsyncedBytes: Long,
    internal val syncedBytes: Long = 0,
    internal val inFlightBytes: Map<String, Long> = emptyMap()
) {
    val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
    val syncByteProgress: SyncByteProgress = SyncByteProgress(
        completedBytes = syncedBytes + inFlightBytes.values.sum(),
        // TODO this solution is a bit hacky to prevent 100% completion before scanning
        //  completes
        // TODO this should be [unsyncedBytes] + [syncedBytes]
        totalBytes = if (isScanningComplete.not()) unsyncedBytes + 1 else unsyncedBytes
    )
}