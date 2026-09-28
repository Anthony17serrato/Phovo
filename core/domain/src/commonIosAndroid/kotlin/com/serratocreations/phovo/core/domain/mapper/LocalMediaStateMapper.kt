package com.serratocreations.phovo.core.domain.mapper

import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.data.photos.local.LocalMediaBackupProgress

fun LocalMediaBackupProgress.toBackupStatus(): BackupStatus {
    return if (this.isScanningComplete.not() && this.totalSyncJobQuantity == 0) {
        BackupStatus.Scanning
    } else if (this.isSyncComplete) {
        BackupStatus.BackupCompleteLocal(
            backedUpQuantity = this.syncedCount,
            // TODO: Implement handling of failed items
            failureQuantity = 0
        )
    } else {
        BackupStatus.LocalMediaBackupProgress(
            syncedCount = syncedCount,
            currentPendingSyncQuantity = currentPendingSyncQuantity
        )
    }
}