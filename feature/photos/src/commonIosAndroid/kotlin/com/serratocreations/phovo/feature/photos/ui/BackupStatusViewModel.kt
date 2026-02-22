package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.core.domain.GetBackupStatusUseCase
import com.serratocreations.phovo.feature.photos.ui.model.BackupActionButton
import com.serratocreations.phovo.feature.photos.ui.model.BackupCompleteUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgressUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatusUiModel
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackupUiModel
import com.serratocreations.phovo.feature.photos.ui.model.ServerOfflineUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.button_view_failed_items

class BackupStatusViewModel(
    private val getBackupStatusUseCase: GetBackupStatusUseCase
): ViewModel() {
    private val actionButton = BackupActionButton(
        Res.string.button_view_failed_items,
        {
            // TODO Define action click
        }
    )

    val backupUiState = getBackupStatusUseCase().map {
        it.toBackupStatus(actionButton)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreparingBackupUiModel
    )
}

fun BackupStatus.toBackupStatus(
    completeActionButton: BackupActionButton
): BackupStatusUiModel {
    return when(this) {
        is BackupStatus.BackupCompleteLocal -> BackupCompleteUiModel(
            backedUpQuantity = this.backedUpQuantity.toLong(),
            failureQuantity = this.failureQuantity.toLong(),
            actionButton = completeActionButton
        )
        is BackupStatus.LocalMediaBackupProgress -> BackupInProgressUiModel(
            syncedCount = this.syncedCount,
            totalCount = this.totalSyncJobQuantity
        )
        BackupStatus.Scanning -> PreparingBackupUiModel
        BackupStatus.ServerOffline -> ServerOfflineUiModel
    }
}