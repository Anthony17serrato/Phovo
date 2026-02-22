package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toBackupStatus
import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.data.photos.IosAndroidLocalMediaManager
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetBackupStatusUseCase(
    private val iosAndroidLocalMediaManager: IosAndroidLocalMediaManager,
    private val remoteMediaRepository: RemoteMediaRepositoryImpl
) {
    operator fun invoke(): Flow<BackupStatus> {
        return combine(
            remoteMediaRepository.observeServerConnection(),
            iosAndroidLocalMediaManager.localMediaState
        ) { isServerConnected, localMediaState ->
            if (isServerConnected.not()) {
                BackupStatus.ServerOffline
            } else {
                localMediaState.toBackupStatus()
            }
        }
    }
}

