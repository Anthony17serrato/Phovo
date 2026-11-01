package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.domain.model.PhovoAppState
import com.serratocreations.phovo.core.domain.GetAppStateUseCase
import com.serratocreations.phovo.feature.photos.ui.model.BackupCompleteUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgressUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupNotSetUpUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatusUiModel
import com.serratocreations.phovo.feature.photos.ui.model.CheckingServerUiModel
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackupUiModel
import com.serratocreations.phovo.feature.photos.ui.model.ServerOfflineUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppStateViewModel(
    private val getAppStateUseCase: GetAppStateUseCase
): ViewModel() {
    val backupUiState = getAppStateUseCase().map {
        it.toAppStateUiModel()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreparingBackupUiModel
    )

    fun onViewFailedItems() {
        // TODO Define action click
    }
}

fun PhovoAppState.toAppStateUiModel(): BackupStatusUiModel {
    return when(this) {
        is PhovoAppState.CheckingServerConnection -> CheckingServerUiModel(
            isProcessing = this.scanningStatus
        )
        is PhovoAppState.Unconfigured -> BackupNotSetUpUiModel(
            isProcessing = this.scanningStatus
        )
        is PhovoAppState.ServerOffline -> ServerOfflineUiModel(
            isProcessing = this.scanningStatus
        )
        PhovoAppState.SyncAwaitingItemsFromProcessingJob -> PreparingBackupUiModel
        is PhovoAppState.LocalMediaBackupProgress -> BackupInProgressUiModel(
            syncedCount = this.syncedCount,
            totalCount = this.totalSyncJobQuantity,
            isProcessing = this.scanningStatus
        )
        is PhovoAppState.BackupCompleteLocal -> BackupCompleteUiModel(
            backedUpQuantity = this.backedUpQuantity.toLong(),
            failureQuantity = this.failureQuantity.toLong()
        )
    }
}
