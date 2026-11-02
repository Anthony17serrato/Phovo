package com.serratocreations.phovo.data.photos.repository.model

import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.model.BackupState

sealed interface SyncState {
    data class Waiting(val serverConnectionState: ServerConnectionState): SyncState

    data class Syncing(
        val syncedCount: Int = 0,
        val currentPendingSyncQuantity: Int,
        internal val unsyncedBytes: Long,
        internal val syncedBytes: Long = 0,
        internal val inFlightBytes: Map<String, Long> = emptyMap()
    ): SyncState {
        val totalSyncJobQuantity: Int = (currentPendingSyncQuantity + syncedCount)

        /**
         * Can reach the total while processing is still finding media, so this alone doesn't mean
         * the backup is done. [BackupState.Active.progress] accounts for that.
         */
        internal fun syncByteProgress(): SyncByteProgress = SyncByteProgress(
            completedBytes = syncedBytes + inFlightBytes.values.sum(),
            totalBytes = unsyncedBytes + syncedBytes
        )
    }

    data class SyncComplete(
        val backedUpQuantity: Int,
        val failureQuantity: Int
    ): SyncState
}