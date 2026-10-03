package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toBackupStatus
import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GetBackupStatusUseCase(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository
) {
    operator fun invoke(): Flow<BackupStatus> {
        return combine(
            localAndRemoteMediaRepository.observeConnectionState(),
            localAndRemoteMediaRepository.syncProgressState
                .map { syncProgressState-> syncProgressState.toBackupStatus() }
                .onStart {
                    emit(BackupStatus.Initializing)
                }
        ) { connectionState, localMediaState ->
            when (connectionState) {
                is ServerConnectionState.Connected -> {
                    localMediaState
                }
                ServerConnectionState.Checking -> BackupStatus.Initializing
                // Unreachable, not yet configured, mid-check, and answered-by-the-wrong-server
                // are all "no backup happening" as far as the user is concerned; none of them imply
                // a different action here.
                is ServerConnectionState.Unreachable,
                // TODO Not configured needs its own backup status that links the user to configure
                ServerConnectionState.NotConfigured,
                ServerConnectionState.IdentityMismatch -> BackupStatus.ServerOffline
            }
        }
    }
}

