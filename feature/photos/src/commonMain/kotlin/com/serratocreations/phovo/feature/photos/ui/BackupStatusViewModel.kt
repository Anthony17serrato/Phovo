package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.feature.photos.ui.model.BackupActionButton
import com.serratocreations.phovo.feature.photos.ui.model.BackupComplete
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgress
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatus
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.button_view_failed_items
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BackupStatusViewModel: ViewModel() {
    private val actionButton = BackupActionButton(
        Res.string.button_view_failed_items,
        {
            // TODO Define action click
        }
    )
    private val _backupUiState = MutableStateFlow<BackupStatus>(PreparingBackup)
    val backupUiState = _backupUiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _backupUiState.update {
                    PreparingBackup
                }
                delay(2.seconds)
                _backupUiState.update {
                    BackupInProgress(
                        syncedCount = 0,
                        totalCount = 9539
                    )
                }
                delay(500.milliseconds)
                _backupUiState.update {
                    BackupInProgress(
                        syncedCount = 2000,
                        totalCount = 9539
                    )
                }
                delay(500.milliseconds)
                _backupUiState.update {
                    BackupInProgress(
                        syncedCount = 4000,
                        totalCount = 9539
                    )
                }
                delay(500.milliseconds)
                _backupUiState.update {
                    BackupInProgress(
                        syncedCount = 6000,
                        totalCount = 9539
                    )
                }
                delay(500.milliseconds)
                _backupUiState.update {
                    BackupInProgress(
                        syncedCount = 9000,
                        totalCount = 9539
                    )
                }
                delay(1.seconds)
                _backupUiState.update {
                    BackupComplete(9539, 5, actionButton)
                }
                delay(1.seconds)
            }
        }
    }
}