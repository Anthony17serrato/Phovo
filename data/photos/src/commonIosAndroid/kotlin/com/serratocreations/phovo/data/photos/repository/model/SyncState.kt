package com.serratocreations.phovo.data.photos.repository.model

import com.serratocreations.phovo.core.model.network.ServerConnectionState

sealed interface SyncState {
    data class Waiting(val serverConnectionState: ServerConnectionState): SyncState

    data class Syncing(
        val syncedCount: Int = 0,
        private val currentPendingSyncQuantity: Int,
        internal val unsyncedBytes: Long,
        internal val syncedBytes: Long = 0,
        internal val inFlightBytes: Map<String, Long> = emptyMap()
    ): SyncState {
        val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)

        // TODO: Progress will probably be computed in BackupState instead as a combination of
        //  both sync and processing
        fun syncByteProgress(isProcessing: Boolean): SyncByteProgress = SyncByteProgress(
            completedBytes = syncedBytes + inFlightBytes.values.sum(),
            // TODO this + 1 solution is a bit hacky to prevent 100% completion before scanning
            //  completes
            totalBytes = (unsyncedBytes + syncedBytes).let { if (isProcessing.not()) it + 1 else it }
        )
    }

    data class SyncComplete(
        val backedUpQuantity: Int,
        val failureQuantity: Int
    ): SyncState
}

data class LocalMediaBackupProgress(
    val syncedCount: Int = 0,
    /**
     * When [isSyncComplete] then this quantity indicates items which failed to sync
     */
    val currentPendingSyncQuantity: Int,
    val isSyncComplete: Boolean = false,
    internal val unsyncedBytes: Long,
    internal val syncedBytes: Long = 0,
    internal val inFlightBytes: Map<String, Long> = emptyMap()
) {
    val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)
}