package com.serratocreations.phovo.core.domain.mapper

import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.data.photos.BackupCompleteLocal
import com.serratocreations.phovo.data.photos.LocalMediaBackupProgress
import com.serratocreations.phovo.data.photos.LocalMediaState
import com.serratocreations.phovo.data.photos.Scanning

fun LocalMediaState.toBackupStatus(): BackupStatus {
    return when(this) {
        is BackupCompleteLocal -> BackupStatus.BackupCompleteLocal(
            backedUpQuantity = backedUpQuantity,
            failureQuantity = failureQuantity
        )
        is LocalMediaBackupProgress -> BackupStatus.LocalMediaBackupProgress(
            syncedCount = syncedCount,
            currentPendingSyncQuantity = currentPendingSyncQuantity,
            isSyncComplete = isSyncComplete
        )
        Scanning -> BackupStatus.Scanning
    }
}