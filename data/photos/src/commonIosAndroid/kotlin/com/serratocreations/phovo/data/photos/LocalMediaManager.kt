package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.util.logTimeToComplete
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.BackupCompleteLocal
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.LocalMediaState
import com.serratocreations.phovo.data.photos.local.Scanning
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.serratocreations.phovo.core.common.PermissionManager
import com.serratocreations.phovo.core.common.PermissionState

class LocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaProcessor: LocalMediaProcessor,
    private val permissionManager: PermissionManager,
    private val appScope: CoroutineScope,
    logger: PhovoLogger,
) {
    companion object {
        private const val TAG = "LocalMediaManager"
    }

    private val log = logger.withTag(TAG)
    private val _localMediaState = MutableStateFlow<LocalMediaState>(Scanning)
    val localMediaState = _localMediaState.asStateFlow()

    /**
     * API initializes job to process local media and synchronize to server.
     * Processing includes tasks such as extracting media metadata and generating md5 hashes and
     * deduplication logic
     */
    fun initMediaProcessing() {
        log.i { "initMediaProcessing" }
        val permissionState = permissionManager.getPermissionState()
        if (permissionState != PermissionState.Granted && permissionState != PermissionState.Limited) {
            log.w { "Gallery permission not granted (state: $permissionState). Skipping local media processing." }
            return
        }
        appScope.launch {
            localAndRemoteMediaRepository.clearNonFailedSyncLogs()
            // todo this approach could lead to OOM ,implement a more memory efficient way to check if media
            //  is already processed(refer to desktop media processing implementation)
            val alreadyProcessedLocalItems = localAndRemoteMediaRepository.phovoMediaFlow().first()
            val processJob = processJob(
                localItems = alreadyProcessedLocalItems,
            )
            // Await server configured before starting sync job
            localAndRemoteMediaRepository.observeServerConnection().filter { it }.first()
            syncJob(processJob)
        }
    }

    private suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localAndRemoteMediaRepository.addOrUpdateMediaItem(mediaItem)
    }

    // Syncs any local media which is still pending sync
    private fun CoroutineScope.syncJob(processingJob: Job) {
        launch {
            launch {
                val syncJob = localAndRemoteMediaRepository.initiateSyncJob(processingJob).await()
                log.i { "syncJob $syncJob" }
                logTimeToComplete(apiTag = "$TAG:syncJob") {
                    syncJob.join()
                }
            }
            localAndRemoteMediaRepository.syncProgressState.onEach { syncStatusUpdate ->
                _localMediaState.update { currentState ->
                    if (syncStatusUpdate.isSyncComplete) {
                        BackupCompleteLocal(
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

    private fun CoroutineScope.processJob(
        localItems: List<MediaItem>
    ) = launch {
        val processMediaChannel = Channel<MediaItem>()
        appScope.consumeProcessedMedia(processMediaChannel)
        with(localMediaProcessor) {
            processLocalItems(
                processedItems = localItems,
                processMediaChannel = processMediaChannel
            ).join()
            processMediaChannel.close()
        }
    }

    private fun CoroutineScope.consumeProcessedMedia(
        processMediaChannel: ReceiveChannel<MediaItem>
    ) = processMediaChannel.consumeAsFlow()
        .onEach(::handleProcessedMediaItem)
        .launchIn(this)
}