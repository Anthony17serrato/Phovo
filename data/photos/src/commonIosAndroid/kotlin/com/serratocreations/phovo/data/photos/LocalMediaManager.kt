package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.common.util.logTimeToComplete
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaProcessor: LocalMediaProcessor,
    private val appScope: CoroutineScope,
    private val cpuBudget: ProcessingCpuBudget,
    logger: PhovoLogger,
) {
    companion object {
        private const val TAG = "LocalMediaManager"
    }

    private val log = logger.withTag(TAG)

    private suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localAndRemoteMediaRepository.addOrUpdateMediaItem(mediaItem)
    }

    // Syncs any local media which is still pending sync
    fun CoroutineScope.syncJob(scanJob: Job) =
        launch {
            val syncJob = localAndRemoteMediaRepository.initiateSyncJob(scanJob).await()
            log.i { "syncJob $syncJob" }
            logTimeToComplete(apiTag = "$TAG:syncJob") {
                syncJob.join()
            }
        }

    fun CoroutineScope.processJob(
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