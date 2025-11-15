package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
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

open class LocalMediaManager(
    private val localMediaRepository: LocalMediaRepository,
    private val localMediaProcessor: LocalMediaProcessor,
    private val appScope: CoroutineScope,
    logger: PhovoLogger,
) {
    private val log = logger.withTag("LocalMediaManager")

    /**
     * API initializes job to process local media and synchronize to server.
     * Processing includes tasks such as extracting media metadata and generating md5 hashes and
     * deduplication logic
     */
    fun initMediaProcessing(localDirectory: String?) {
        log.i { "initMediaProcessing" }
        appScope.launch {
            // todo in the future paging and db queries should be used to prevent OOM on larger media libraries
            val alreadyProcessedLocalItems = localMediaRepository.phovoMediaFlow().first()
            val processJob = processJob(
                localDirectory = localDirectory,
                localItems = alreadyProcessedLocalItems,
            )
            syncJob(processJob)
        }
    }

    private suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localMediaRepository.addOrUpdateMediaItem(mediaItem)
    }

    // Syncs any local media which is still pending sync
    protected open fun CoroutineScope.syncJob(processJob: Job) {
        // TODO Server may eventually support syncing to other servers, for now it is not supported
    }

    private fun CoroutineScope.processJob(
        localDirectory: String?,
        localItems: List<MediaItem>
    ) = launch {
        val processMediaChannel = Channel<MediaItem>()
        appScope.consumeProcessedMedia(processMediaChannel)
        with(localMediaProcessor) {
            processLocalItems(
                processedItems = localItems,
                localDirectory = localDirectory,
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