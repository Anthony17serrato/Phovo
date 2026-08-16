package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toBackupStatus
import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.core.model.network.NetworkFailure
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetBackupStatusUseCase(
    private val localMediaManager: LocalMediaManager,
    private val remoteMediaRepository: RemoteMediaRepositoryImpl
) {
    operator fun invoke(): Flow<BackupStatus> {
        return combine(
            remoteMediaRepository.observeConnectionState(),
            localMediaManager.localMediaState
        ) { connectionState, localMediaState ->
            when (connectionState) {
                is ServerConnectionState.Connected -> localMediaState.toBackupStatus()
                is ServerConnectionState.Unreachable ->
                    BackupStatus.ServerOffline(reason = connectionState.reason)
                // Answering, but not the server we paired with — treated as offline so no photos
                // are uploaded to it.
                is ServerConnectionState.IdentityMismatch ->
                    BackupStatus.ServerOffline(reason = NetworkFailure.Unreachable)
                // Not configured, still checking, or currently being located — nothing to report.
                ServerConnectionState.Unknown,
                ServerConnectionState.Checking,
                ServerConnectionState.Resolving -> BackupStatus.ServerOffline(reason = null)
            }
        }
    }
}

