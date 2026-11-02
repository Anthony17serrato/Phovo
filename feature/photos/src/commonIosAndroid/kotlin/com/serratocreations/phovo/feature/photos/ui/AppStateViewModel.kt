package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.domain.model.PhovoAppState
import com.serratocreations.phovo.core.domain.GetAppStateUseCase
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.photos.model.ProcessingState
import com.serratocreations.phovo.feature.photos.ui.model.BackupCompleteUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgressUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupNotSetUpUiModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatusUiModel
import com.serratocreations.phovo.feature.photos.ui.model.CheckingServerUiModel
import com.serratocreations.phovo.feature.photos.ui.model.MediaAccessRequiredUiModel
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackupUiModel
import com.serratocreations.phovo.feature.photos.ui.model.ProcessingCounts
import com.serratocreations.phovo.feature.photos.ui.model.ServerOfflineUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppStateViewModel(
    private val getAppStateUseCase: GetAppStateUseCase,
    private val permissionRepository: PermissionRepository
): ViewModel() {
    val backupUiState = getAppStateUseCase().map {
        it.toAppStateUiModel()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreparingBackupUiModel(ProcessingCounts(processedCount = 0, foundCount = 0))
    )

    fun onAllowPhotoAccess() {
        viewModelScope.launch {
            val galleryPermissionStatus = permissionRepository.observeGalleryPermissionStatus().first()
            // The system won't show the prompt again once access is permanently denied.
            if (galleryPermissionStatus.permissionStatus == PermissionStatus.PermanentlyDenied) {
                permissionRepository.openSystemPermissionSettings()
            } else {
                permissionRepository.requestGalleryPermissions()
            }
        }
    }

    fun onViewFailedItems() {
        // TODO Define action click
    }
}

fun PhovoAppState.toAppStateUiModel(): BackupStatusUiModel {
    return when(this) {
        PhovoAppState.MediaAccessRequired -> MediaAccessRequiredUiModel
        is PhovoAppState.CheckingServerConnection -> CheckingServerUiModel(
            processing = this.processing.toProcessingCounts()
        )
        is PhovoAppState.Unconfigured -> BackupNotSetUpUiModel(
            processing = this.processing.toProcessingCounts()
        )
        is PhovoAppState.ServerOffline -> ServerOfflineUiModel(
            processing = this.processing.toProcessingCounts()
        )
        is PhovoAppState.SyncAwaitingItemsFromProcessingJob -> PreparingBackupUiModel(
            processing = this.processing.toProcessingCounts()
        )
        is PhovoAppState.LocalMediaBackupProgress -> BackupInProgressUiModel(
            syncedCount = this.syncedCount,
            totalCount = this.totalSyncJobQuantity,
            processing = this.processing.toProcessingCounts()
        )
        is PhovoAppState.BackupCompleteLocal -> BackupCompleteUiModel(
            backedUpQuantity = this.backedUpQuantity.toLong(),
            failureQuantity = this.failureQuantity.toLong()
        )
    }
}

private fun ProcessingState.Running.toProcessingCounts() = ProcessingCounts(
    processedCount = progress.processedCount,
    foundCount = progress.foundCount
)

/** Null once processing is done. */
private fun ProcessingState.toProcessingCounts(): ProcessingCounts? = when (this) {
    is ProcessingState.Running -> toProcessingCounts()
    is ProcessingState.Done -> null
}
