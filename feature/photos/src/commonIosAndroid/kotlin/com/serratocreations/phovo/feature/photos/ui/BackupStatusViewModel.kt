package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.photos.IosAndroidLocalMediaManager
import com.serratocreations.phovo.data.photos.MediaBackupProgress
import com.serratocreations.phovo.data.photos.MediaBackupStatus
import com.serratocreations.phovo.data.photos.Scanning
import com.serratocreations.phovo.feature.photos.ui.model.BackupActionButton
import com.serratocreations.phovo.feature.photos.ui.model.BackupComplete
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgress
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatus
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.button_view_failed_items

class BackupStatusViewModel(
    private val iosAndroidLocalMediaManager: IosAndroidLocalMediaManager
): ViewModel() {
    private val actionButton = BackupActionButton(
        Res.string.button_view_failed_items,
        {
            // TODO Define action click
        }
    )

    val backupUiState = iosAndroidLocalMediaManager.localMediaState.map {
        it.toBackupStatus(actionButton)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreparingBackup
    )
}

fun MediaBackupStatus.toBackupStatus(
    completeActionButton: BackupActionButton
): BackupStatus {
    return when(this) {
        is com.serratocreations.phovo.data.photos.BackupComplete -> {
            BackupComplete(
                backedUpQuantity = this.backedUpQuantity.toLong(),
                failureQuantity = this.failureQuantity.toLong(),
                actionButton = completeActionButton
            )
        }
        is MediaBackupProgress -> {
            BackupInProgress(
                syncedCount = this.syncedCount,
                totalCount = this.totalSyncJobQuantity
            )
        }
        Scanning -> PreparingBackup
    }
}