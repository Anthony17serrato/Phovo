package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toBackupStatus
import com.serratocreations.phovo.core.domain.model.BackupStatus
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
                // Unreachable, not yet configured, mid-check, and answered-by-the-wrong-server
                // are all "no backup happening" as far as the user is concerned; none of them imply
                // a different action here.
                is ServerConnectionState.Unreachable,
                ServerConnectionState.Unknown,
                ServerConnectionState.Checking,
                ServerConnectionState.IdentityMismatch -> BackupStatus.ServerOffline
            }
        }
    }
}

