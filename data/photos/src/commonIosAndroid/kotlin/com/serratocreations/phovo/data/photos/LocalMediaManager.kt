package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.util.logTimeToComplete
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.isConnected
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.SyncByteProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaProcessor: LocalMediaProcessor,
    private val appScope: CoroutineScope,
    logger: PhovoLogger,
) {
    companion object {
        private const val TAG = "LocalMediaManager"
    }

    private val log = logger.withTag(TAG)

    // TODO this needs to converge with sync progress state
    /**
     * Backup progress in bytes. Sync is usually faster than the scan, so while the scan is still
     * running the total carries one extra byte: sync catching up with everything found so far then
     * reads as just short of done rather than done.
     */
    val backupByteProgress: Flow<SyncByteProgress> = combine(
        localAndRemoteMediaRepository.syncByteProgress,
        localAndRemoteMediaRepository.syncProgressState.map { it.isScanningComplete }
    ) { bytes, isScanningComplete ->
        if (isScanningComplete.not()) bytes.copy(totalBytes = bytes.totalBytes + 1) else bytes
    }.distinctUntilChanged()

    // TODO this logic needs to be improved once periodic sync is being implemented
    //  https://github.com/Anthony17serrato/Phovo/issues/124
    /**
     * API initializes job to process local media and synchronize to server.
     * Processing includes tasks such as extracting media metadata and generating md5 hashes and
     * deduplication logic
     */
    fun CoroutineScope.initMediaProcessing() = launch {
        // todo this approach could lead to OOM ,implement a more memory efficient way to check if media
        //  is already processed(refer to desktop media processing implementation)
        val alreadyProcessedLocalItems = localAndRemoteMediaRepository.phovoMediaFlow().first()
        val processingJob = processJob(
            localItems = alreadyProcessedLocalItems,
        )
        // Await server configured before starting sync job
        localAndRemoteMediaRepository.observeConnectionState().first { it.isConnected }
        syncJob(processingJob)
        processingJob.join()
    }

    private suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localAndRemoteMediaRepository.addOrUpdateMediaItem(mediaItem)
    }

    // Syncs any local media which is still pending sync
    private fun CoroutineScope.syncJob(scanJob: Job) =
        launch {
            val syncJob = localAndRemoteMediaRepository.initiateSyncJob(scanJob).await()
            log.i { "syncJob $syncJob" }
            logTimeToComplete(apiTag = "$TAG:syncJob") {
                syncJob.join()
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