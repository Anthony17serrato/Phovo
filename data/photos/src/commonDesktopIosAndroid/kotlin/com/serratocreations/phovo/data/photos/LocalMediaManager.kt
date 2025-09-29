package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

open class LocalMediaManager(
    private val mediaRepository: LocalSupportMediaRepository,
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
            val alreadyProcessedLocalItems = mediaRepository.phovoMediaFlow().first()
            processJob(
                localDirectory,
                alreadyProcessedLocalItems
            )
            syncJob(alreadyProcessedLocalItems)
        }
    }

    protected open suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        mediaRepository.addMediaItem(mediaItem)
    }

    // Syncs any local media which is still pending sync
    protected open fun CoroutineScope.syncJob(localItems: List<MediaItem>) {
        // TODO Server may eventually support syncing to other servers, for now it is not supported
    }

    private fun CoroutineScope.processJob(
        localDirectory: String?,
        localItems: List<MediaItem>
    ) = launch {
        val processMediaChannel = Channel<MediaItem>()
        with(localMediaProcessor) {
            processLocalItems(
                processedItems = localItems,
                localDirectory = localDirectory,
                processMediaChannel = processMediaChannel
            )
            consumeProcessedMedia(processMediaChannel)
        }
    }

    private fun CoroutineScope.consumeProcessedMedia(
        processMediaChannel: ReceiveChannel<MediaItem>
    ) = processMediaChannel.consumeAsFlow()
        .onEach(::handleProcessedMediaItem)
        .launchIn(this)
}