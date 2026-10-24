package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.isConnected
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
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

    private data class BackupState(val isProcessing: Boolean, val isSyncing: Boolean) {
        val isIdle: Boolean get() = !isProcessing && !isSyncing
    }

    private val log = logger.withTag(TAG)

    // flowOf(Unit) stands in for a processing trigger data source, e.g. one that emits when new
    // media is added to the library. Until that exists, a single pass runs per process.
    //
    // flatMapLatest restarts the pass on every trigger, cancelling processing and any sync in
    // flight. That is deliberate: a running pass scans from a query taken when it started, so
    // media added since would otherwise be missed. It is not efficient though: each restart
    // re-scans and re-hashes the whole library (#171), and an upload cancelled mid-file starts
    // over from the beginning on the next pass.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val backupState: SharedFlow<BackupState> = flowOf(Unit)
        .flatMapLatest { backupPass() }
        .shareIn(appScope, SharingStarted.Eagerly, replay = 1)

    suspend fun awaitBackupIdle() {
        backupState.first { it.isIdle }
    }

    private fun backupPass(): Flow<BackupState> = channelFlow {
        val processing = launch { processLocalMedia() }
        localAndRemoteMediaRepository.observeConnectionState()
            .collectLatest { server ->
                if (server.isConnected) {
                    send(BackupState(isProcessing = processing.isActive, isSyncing = true))
                    localAndRemoteMediaRepository.sync(processing)
                }
                send(BackupState(isProcessing = processing.isActive, isSyncing = false))
                processing.join()
                send(BackupState(isProcessing = false, isSyncing = false))
            }
    }

    private suspend fun processLocalMedia() = coroutineScope {
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