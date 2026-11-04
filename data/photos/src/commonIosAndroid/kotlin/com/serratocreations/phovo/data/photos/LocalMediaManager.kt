package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.isConnected
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.local.model.ScanEvent
import com.serratocreations.phovo.data.photos.model.BackupState
import com.serratocreations.phovo.data.photos.model.ProcessingProgress
import com.serratocreations.phovo.data.photos.model.ProcessingState
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.SyncState
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class LocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaProcessor: LocalMediaProcessor,
    private val fileHashCalculator: FileHashCalculator,
    private val appScope: CoroutineScope,
    private val cpuBudget: ProcessingCpuBudget,
    private val permissionRepository: PermissionRepository,
    logger: PhovoLogger,
) {
    companion object {
        private const val TAG = "LocalMediaManager"
    }

    private val log = logger.withTag(TAG)

    private val scanRequests = MutableSharedFlow<Unit>(replay = 1)

    // Gallery access gates every pass, whoever reads this first. Without it a pass would scan
    // nothing, then report everything as backed up. Granting access starts a pass, and revoking it
    // cancels the one in flight.
    //
    // scanRequests feeds incoming processing requests and acts as the initializing event for backups
    //
    // flatMapLatest restarts the pass on every trigger, cancelling processing and any sync in
    // flight. That is deliberate: a running pass scans from a query taken when it started, so
    // media added since would otherwise be missed. It is not efficient though: each restart
    // re-scans and re-hashes the whole library (#171), and an upload cancelled mid-file starts
    // over from the beginning on the next pass.
    @OptIn(ExperimentalCoroutinesApi::class)
    val backupState: SharedFlow<BackupState> = permissionRepository.observeGalleryPermissionStatus()
        .map { it.permissionStatus == PermissionStatus.Granted || it.isLimited }
        .distinctUntilChanged()
        .flatMapLatest { canReadGallery ->
            if (canReadGallery) {
                scanRequests.onStart { emit(Unit) }.flatMapLatest { backupPass() }
            } else {
                flowOf(BackupState.MediaAccessRequired)
            }
        }
        .shareIn(appScope, SharingStarted.Eagerly, replay = 1)

    suspend fun joinOrStartBackup() {
        // Without gallery access processing never starts, so there is nothing to join.
        if (backupState.first() is BackupState.MediaAccessRequired) return
        coroutineScope {
            val awaitProcessingStart = launch(start = CoroutineStart.UNDISPATCHED) {
                // Access can also be revoked before processing starts.
                backupState.first {
                    it is BackupState.MediaAccessRequired || (it is BackupState.Active && it.isProcessing)
                }
            }
            val currentState = backupState.first()
            if (currentState is BackupState.Active && currentState.isProcessing.not()) {
                scanRequests.emit(Unit)
            }
            awaitProcessingStart.join()
            // run until idle
            backupState.first { it.isIdle }
        }
    }

    // Processing and sync each update the state on their own, so processing progress keeps moving
    // while sync waits for the server, and sync progress keeps moving after processing is done.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun backupPass(): Flow<BackupState> = channelFlow {
        // Shared once: combine and sync both read it, and collecting processLocalMedia() twice would
        // process the library twice.
        val processing = processLocalMedia()
            .stateIn(this, SharingStarted.Eagerly, ProcessingState.Running())
        val moreMediaExpected = processing.map { it is ProcessingState.Running }
        val syncStates = localAndRemoteMediaRepository.observeConnectionState()
            .flatMapLatest { server ->
                if (server.isConnected) {
                    localAndRemoteMediaRepository.sync(moreMediaExpected)
                } else {
                    flowOf(SyncState.Waiting(server))
                }
            }
        combine(processing, syncStates) { processingState, syncState ->
            BackupState.Active(processing = processingState, syncState = syncState)
        }.collect { send(it) }
    }

    /**
     * Processes the local library while collected. Emits [ProcessingState.Running] as progress
     * changes, then a single [ProcessingState.Done] once every item is stored, and completes.
     */
    private fun processLocalMedia(): Flow<ProcessingState> = channelFlow {
        val processMediaChannel = Channel<PlatformFile>()
        val onMediaProcessedChannel = Channel<Unit>()

        val workers = List(cpuBudget.workerCount) {
            processMediaWorker(processMediaChannel, onMediaProcessedChannel)
        }
        // Closed once every worker is done, which ends processedCount
        launch {
            workers.joinAll()
            onMediaProcessedChannel.close()
        }
        data class ScanCounts(val found: Int = 0, val skipped: Int = 0)

        val scanCounts = localMediaProcessor.scanLocalItems()
            // Suspends while every worker is busy, which holds the scan back
            .onEach { if (it is ScanEvent.Item) processMediaChannel.send(it.file) }
            .onCompletion { processMediaChannel.close() }
            .runningFold(ScanCounts()) { counts, event ->
                when (event) {
                    is ScanEvent.Found -> counts.copy(found = event.count)
                    ScanEvent.Skipped -> counts.copy(skipped = counts.skipped + 1)
                    is ScanEvent.Item -> counts
                }
            }
            .distinctUntilChanged()
        val processedCount = onMediaProcessedChannel.consumeAsFlow().runningFold(0) { acc, _ -> acc + 1 }

        // Completes once the scan and every worker are done, so everything processed is stored
        val lastProgress = combine(scanCounts, processedCount) { scan, processed ->
            ProcessingProgress(processedCount = processed + scan.skipped, foundCount = scan.found)
        }
            .onEach { send(ProcessingState.Running(it)) }
            .last()
        send(ProcessingState.Done(lastProgress))
    }

    private fun CoroutineScope.processMediaWorker(
        processMediaReceiveChannel: ReceiveChannel<PlatformFile>,
        onMediaProcessedSendChannel: SendChannel<Unit>
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
            onMediaProcessedSendChannel.send(Unit)
        }
    }
}