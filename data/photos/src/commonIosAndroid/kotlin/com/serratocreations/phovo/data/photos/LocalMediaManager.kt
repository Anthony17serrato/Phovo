package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.common.util.logTimeToComplete
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class LocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaProcessor: LocalMediaProcessor,
    private val fileHashCalculator: FileHashCalculator,
    private val appScope: CoroutineScope,
    private val cpuBudget: ProcessingCpuBudget,
    logger: PhovoLogger,
) {
    companion object {
        private const val TAG = "LocalMediaManager"
    }

    private val log = logger.withTag(TAG)

    // Syncs any local media which is still pending sync
    fun CoroutineScope.syncJob(scanJob: Job) =
        launch {
            logTimeToComplete(apiTag = "$TAG:syncJob") {
                localAndRemoteMediaRepository.sync(scanJob)
            }
        }

    fun CoroutineScope.processJob() = launch {
        val processMediaChannel = Channel<PlatformFile>()

        repeat(cpuBudget.workerCount) {
            processMediaWorker(processMediaChannel)
        }

        localMediaProcessor.scanLocalItems(
            processMediaSendChannel = processMediaChannel
        )
        processMediaChannel.close()
    }

    private fun CoroutineScope.processMediaWorker(
        processMediaReceiveChannel: ReceiveChannel<PlatformFile>
    ) = launch {
        for (fileToProcess in processMediaReceiveChannel) {
            yield()
            val assetHash = fileHashCalculator.computeSha256(fileToProcess)
            val isAssetAlreadyProcessed = localAndRemoteMediaRepository
                .getMediaItemByAssetHash(assetHash)
                .let { it != null }
            if (isAssetAlreadyProcessed.not()) {
                when (val result = localMediaProcessor.processItem(fileToProcess, assetHash)) {
                    is MediaProcessingResult.Failure -> {
                        // TODO save failure to DB table
                        log.e { "Processing failed ${result.reason}" }
                    }
                    is MediaProcessingResult.Success -> {
                        localAndRemoteMediaRepository.addOrUpdateMediaItem(result.mediaItem)
                    }
                }
            }
        }
    }
}