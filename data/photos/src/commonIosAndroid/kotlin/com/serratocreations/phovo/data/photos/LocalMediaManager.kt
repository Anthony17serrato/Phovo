package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.util.logTimeToComplete
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.isConnected
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
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