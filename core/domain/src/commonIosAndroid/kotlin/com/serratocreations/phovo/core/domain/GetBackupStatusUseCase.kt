package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toBackupStatus
import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GetBackupStatusUseCase(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaManager: LocalMediaManager
) {
    operator fun invoke(): Flow<BackupStatus> {
        return combine(
            localAndRemoteMediaRepository.observeConnectionState(),
            localAndRemoteMediaRepository.syncProgressState
                .map { syncProgressState-> syncProgressState.toBackupStatus() }
                .onStart {
                    emit(BackupStatus.Initializing)
                },
            localMediaManager.backupState
        ) { connectionState, localMediaState, backupState ->
            when {
                connectionState is ServerConnectionState.Connected -> {
                    localMediaState
                }
                connectionState == ServerConnectionState.Checking -> BackupStatus.Initializing
                backupState.isProcessing -> BackupStatus.Processing
                connectionState == ServerConnectionState.NotConfigured -> BackupStatus.Unconfigured
                // Unreachable, not yet configured, mid-check, and answered-by-the-wrong-server
                // are all "no backup happening" as far as the user is concerned; none of them imply
                // a different action here.
                connectionState is ServerConnectionState.Unreachable ||
                connectionState == ServerConnectionState.IdentityMismatch -> BackupStatus.ServerOffline
                else -> BackupStatus.ServerOffline
            }
        }
    }
}

