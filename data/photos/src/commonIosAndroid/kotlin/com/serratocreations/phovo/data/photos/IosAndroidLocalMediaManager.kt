package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IosAndroidLocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    localMediaRepository: LocalMediaRepository,
    localMediaProcessor: LocalMediaProcessor,
    appScope: CoroutineScope,
    logger: PhovoLogger,
): LocalMediaManager(
    localMediaRepository,
    localMediaProcessor,
    appScope,
    logger
) {
    private val _localMediaState = MutableStateFlow<MediaBackupStatus>(Scanning)
    val localMediaState = _localMediaState.asStateFlow()

    override fun CoroutineScope.syncJob(processJob: Job) {
        launch {
            processJob.join()
            localAndRemoteMediaRepository.initiateSyncJob()
            localAndRemoteMediaRepository.syncProgressState.onEach { syncStatusUpdate ->
                _localMediaState.update { currentState ->
                    if (syncStatusUpdate.isSyncComplete) {
                        BackupComplete(
                            backedUpQuantity = syncStatusUpdate.syncedCount,
                            // TODO: Implement handling of failed items
                            failureQuantity = 0
                        )
                    } else {
                        syncStatusUpdate
                    }
                }
            }.launchIn(this)
        }
    }
}