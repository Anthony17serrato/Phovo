package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.isConnected
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.model.BackupState
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.SyncState
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
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

    private fun backupPass(): Flow<BackupState> = channelFlow {
        val processing = launch { processLocalMedia() }
        localAndRemoteMediaRepository.observeConnectionState()
            .collectLatest { server ->
                var currentSyncState: SyncState = SyncState.Waiting(server)
                if (server.isConnected) {
                    localAndRemoteMediaRepository.sync(processing).collect { syncState ->
                        currentSyncState = syncState
                        send(BackupState.Active(isProcessing = processing.isActive, syncState = syncState))
                    }
                } else {
                    send(
                        BackupState.Active(
                            isProcessing = processing.isActive,
                            syncState = currentSyncState
                        )
                    )
                }
                processing.join()
                send(BackupState.Active(isProcessing = false, syncState = currentSyncState))
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