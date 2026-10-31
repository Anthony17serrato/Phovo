package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemMetadataEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.mappers.toMediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.isConnected
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.SyncImage
import com.serratocreations.phovo.data.photos.repository.model.SyncQueueable
import com.serratocreations.phovo.data.photos.repository.model.SyncState
import com.serratocreations.phovo.data.photos.repository.model.SyncVideo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

interface LocalAndRemoteMediaRepository: LocalMediaRepository, RemoteMediaRepository {
//    /**
//     * Progress of the current sync job. Emits nothing until the first sync job in this process
//     * starts. Once a job finishes, its final state is kept, and new collectors get it, until the
//     * next job starts and resets it.
//     */
//    val syncProgressState: Flow<LocalMediaBackupProgress>

    /**
     * Adds the [syncQueueable] to the sync queue. media added using this API gets picked up by
     * sync workers in a first in first out fashion.
     */
    suspend fun syncMedia(syncQueueable: SyncQueueable)

    /**
     * Runs an application wide sync and suspends until it completes. Cancelling the caller cancels
     * the sync. This API should typically be initiated by a work manager(or equivalent platform API)
     *
     * Only one sync runs at a time. A call made while another sync is running waits for it to
     * finish, then runs its own, so an overlapping run (such as a replaced worker still cancelling)
     * never mixes its progress into the next one.
     *
     * @param scanJob a reference to the Job which is scanning for new media, while the scan job is
     * active sync workers will remain running.
     */
    suspend fun sync(scanJob: Job): Flow<SyncState>
}

class LocalAndRemoteMediaRepositoryImpl(
    private val localMediaRepository: LocalMediaRepository,
    private val remoteMediaRepository: RemoteMediaRepository,
    private val applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    logger: PhovoLogger
): LocalAndRemoteMediaRepository {
    companion object {
        private const val SYNC_VIDEO_WORKER_COUNT = 2
        private const val SYNC_IMAGE_WORKER_COUNT = 4
        private val WORKER_IDLE_DELAY = 1.seconds
    }
    private val log = logger.withTag("LocalAndRemoteMediaRepository")

    private val syncImageRequestChannel = Channel<String>(Channel.RENDEZVOUS)
    private val syncVideoRequestChannel = Channel<String>(Channel.RENDEZVOUS)

//    private val _syncProgressState = MutableStateFlow<LocalMediaBackupProgress?>(null)
//    override val syncProgressState = _syncProgressState.filterNotNull()

    init {
        repeat(SYNC_IMAGE_WORKER_COUNT) {
            applicationScope.syncWorker(syncImageRequestChannel)
        }
        repeat(SYNC_VIDEO_WORKER_COUNT) {
            applicationScope.syncWorker(syncVideoRequestChannel)
        }
    }

    /**
     * @return null if there are no remaining unsynced items for the media type
     */
    private suspend fun getNextUnsyncedItem(type: MediaType): LocalMediaItemWithMetadata? {
        var syncCandidate: LocalMediaItemWithMetadata?
        do {
            yield()
            syncCandidate = localMediaRepository.getNextUnsyncedItemExcludingUuidSet(
                mediaType = type
            ) ?: return null // No remaining items to sync
            val isAddedToSync =
                localMediaRepository.claimItemForSync(syncCandidate.mediaItemMetadataEntity.assetHash)
        } while (isAddedToSync.not())
        return syncCandidate
    }

    private fun CoroutineScope.syncWorker(
        type: MediaType,
        processingJob: Job,
        inFlightMediaItemProgressSendChannel: SendChannel<InFlightMediaItemProgress>
    ) = launch {
        while (this.isActive) {
            val wasProcessing = processingJob.isActive
            val nextUnsyncedItem = getNextUnsyncedItem(type) ?: run {
                // Keep worker running if processing job has not completed
                if (wasProcessing) {
                    delay(WORKER_IDLE_DELAY)
                    continue
                } else {
                    return@launch
                }
            }
            log.i { "claimed item hash ${nextUnsyncedItem.mediaItemMetadataEntity.assetHash}" }
            // Do not sync if server is not connected
            remoteMediaRepository.observeConnectionState().first { it.isConnected }
            val assetHash = nextUnsyncedItem.mediaItemMetadataEntity.assetHash
            log.i { "starting sync for item hash $assetHash" }
            // Terminate the worker if there is no remaining items to sync
            val result = sync(nextUnsyncedItem, inFlightMediaItemProgressSendChannel)
            log.i { "sync complete for item hash $assetHash result $result" }
            when (result) {
                is NetworkResult.NetworkError -> {
                    localMediaRepository.addSyncFailure(
                        assetHash = assetHash,
                        errorMessage = result.message
                    )
                }

                is NetworkResult.NetworkSuccess -> {
                    localMediaRepository.removeSyncAsset(nextUnsyncedItem.mediaItemMetadataEntity.assetHash)
                }
            }
        }
    }

    private fun CoroutineScope.syncWorker(
        syncReceiveChannel: ReceiveChannel<String>
    ) = launch {
        for (assetHash in syncReceiveChannel) {
            yield()
            // TODO check assetHash is not being processed by other workers and add to set of uuids
            //  which are being currently processed
            val localMediaItemWithMetadata =
                localMediaRepository.getLocalMediaItemWithMetadataByAssetHash(assetHash)
            localMediaItemWithMetadata?.let { mediaItemEntityNotNull ->
                sync(mediaItemEntityNotNull)
            }
        }
    }

    private sealed interface InFlightMediaItemProgress {
        val assetHash: String
        data class Progress(
            override val assetHash: String,
            val bytesSynced: Long
        ): InFlightMediaItemProgress

        data class Complete(
            override val assetHash: String,
            /** The file's size when the upload succeeded, 0 when it failed or was cancelled */
            val syncedBytes: Long
        ) : InFlightMediaItemProgress
    }

    private suspend fun sync(
        mediaItemEntity: LocalMediaItemWithMetadata,
        /** Optional param for receiving sync progress updates **/
        inFlightMediaItemProgressSendChannel: SendChannel<InFlightMediaItemProgress>? = null
    ): NetworkResult<Unit> {
        val metadata = mediaItemEntity.mediaItemMetadataEntity
        val assetHash = metadata.assetHash
        val result = remoteMediaRepository.syncMedia(
            media = metadata.toMediaItemDto(),
            mediaUri = mediaItemEntity.localLocation.localUri,
            onBytesSent = { bytesSent ->
                // Capped at the recorded size, so a file whose stored size is out of date
                // can't push completed past total.
                inFlightMediaItemProgressSendChannel?.trySend(
                    InFlightMediaItemProgress.Progress(
                        assetHash = assetHash,
                        bytesSynced = bytesSent.coerceAtMost(metadata.size)
                    )
                )
            }
        )

        inFlightMediaItemProgressSendChannel?.trySend(
            InFlightMediaItemProgress.Complete(
                assetHash = assetHash,
                syncedBytes = if (result is NetworkResult.NetworkSuccess) metadata.size else 0
            )
        )

        log.i { "sync complete result $result hash $assetHash" }
        if (result is NetworkResult.NetworkSuccess) {
            localMediaRepository.markAsSynced(assetHash = assetHash)
        }
        log.i { "marked as synced ${mediaItemEntity.mediaItemMetadataEntity.assetHash}" }
        return result
    }

    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        val remoteItemsFlow = remoteMediaRepository.phovoMediaFlow()
        val localItemsFlow = localMediaRepository.phovoMediaFlow()

        return combine(remoteItemsFlow, localItemsFlow) { remote, local ->
            (local + remote).distinctBy { it.uniqueAssetIdentifier }
        }.flowOn(ioDispatcher)
    }

    override suspend fun syncMedia(syncQueueable: SyncQueueable) {
        when (syncQueueable) {
            is SyncImage -> syncImageRequestChannel.send(syncQueueable.uuid)
            is SyncVideo -> syncVideoRequestChannel.send(syncQueueable.uuid)
        }
    }

    // Held for the whole sync, so only one runs at a time
    private val syncMutex = Mutex()
    override suspend fun sync(scanJob: Job): Flow<SyncState> = channelFlow {
        syncMutex.withLock {
            clearNonFailedSyncLogs()
            val initialUnsynced = observeUnsyncedMedia().first()
            val inFlightBytes = mutableMapOf<String, Long>()
            send(
                SyncState.Syncing(
                    currentPendingSyncQuantity = initialUnsynced.count,
                    unsyncedBytes = initialUnsynced.bytes
                )
            )
            val inFlightMediaItemProgressChannel = Channel<InFlightMediaItemProgress>(Channel.UNLIMITED)

            val progressJob = combine(
                observeUnsyncedMedia(),
                inFlightMediaItemProgressChannel.consumeAsFlow()
            ) { unsynced, progress ->
                when (progress) {
                    is InFlightMediaItemProgress.Complete -> {
                        inFlightBytes.remove(progress.assetHash)
                    }
                    is InFlightMediaItemProgress.Progress -> {
                        inFlightBytes[progress.assetHash] = progress.bytesSynced
                    }
                }
                send(
                    SyncState.Syncing(
                        syncedCount = initialUnsynced.count - unsynced.count,
                        currentPendingSyncQuantity = unsynced.count,
                        unsyncedBytes = unsynced.bytes,
                        syncedBytes = initialUnsynced.bytes - unsynced.bytes,
                        inFlightBytes = inFlightBytes
                    )
                )
            }.launchIn(this)

            val syncJobs = mutableListOf<Job>()
            repeat(SYNC_IMAGE_WORKER_COUNT) {
                syncJobs.add(
                    syncWorker(
                        type = MediaType.Image,
                        processingJob = scanJob,
                        inFlightMediaItemProgressSendChannel = inFlightMediaItemProgressChannel
                    )
                )
            }
            repeat(SYNC_VIDEO_WORKER_COUNT) {
                syncJobs.add(
                    syncWorker(
                        type = MediaType.Video,
                        processingJob = scanJob,
                        inFlightMediaItemProgressSendChannel = inFlightMediaItemProgressChannel
                    )
                )
            }
            scanJob.join()
            joinAll(*syncJobs.toTypedArray())
            progressJob.cancel()

            val finalUnsynced = observeUnsyncedMedia().first()
            send(
                SyncState.SyncComplete(
                    backedUpQuantity = initialUnsynced.count - finalUnsynced.count,
                    failureQuantity = finalUnsynced.count
                )
            )
        }
    }.flowOn(defaultDispatcher)

    override suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String,
        onBytesSent: (bytesSent: Long) -> Unit
    ) = remoteMediaRepository.syncMedia(media, mediaUri, onBytesSent)

    override fun observeConnectionState() = remoteMediaRepository.observeConnectionState()

    override suspend fun getMediaItemByAssetHash(assetHash: String) = localMediaRepository.getMediaItemByAssetHash(assetHash)

    override suspend fun getLocalMediaItemWithMetadataByAssetHash(assetHash: String): LocalMediaItemWithMetadata? {
        return localMediaRepository.getLocalMediaItemWithMetadataByAssetHash(assetHash)
    }

    override suspend fun getLocalMediaByAssetHash(assetHash: String): LocalMediaEntity? {
        return localMediaRepository.getLocalMediaByAssetHash(assetHash = assetHash)
    }

    override suspend fun doesCompleteAssetExist(assetHash: String): Boolean =
        localMediaRepository.doesCompleteAssetExist(assetHash = assetHash)

    override suspend fun observeFirstUnprocessedFullLocalMedia(): Flow<LocalMediaEntity?> =
        localMediaRepository.observeFirstUnprocessedFullLocalMedia()

    override suspend fun tryProcessingClaim(assetHash: String): Boolean =
        localMediaRepository.tryProcessingClaim(assetHash)

    override suspend fun removeProcessingClaim(assetHash: String) =
        localMediaRepository.removeProcessingClaim(assetHash = assetHash)

    override suspend fun addOrUpdateMediaItem(mediaItem: MediaItem) = localMediaRepository.addOrUpdateMediaItem(mediaItem)

    override suspend fun addOrUpdateLocalMediaItem(localMediaEntity: LocalMediaEntity) =
        localMediaRepository.addOrUpdateLocalMediaItem(localMediaEntity)

    override fun observeUnsyncedMedia() = localMediaRepository.observeUnsyncedMedia()

    override suspend fun updateMediaItem(mediaItemMetadataEntity: MediaItemMetadataEntity) =
        localMediaRepository.updateMediaItem(mediaItemMetadataEntity)

    override suspend fun getNextUnsyncedItemExcludingUuidSet(
        mediaType: MediaType
    ) = localMediaRepository.getNextUnsyncedItemExcludingUuidSet(mediaType)

    override suspend fun markAsSynced(assetHash: String) {
        localMediaRepository.markAsSynced(assetHash)
    }

    override suspend fun claimItemForSync(assetHash: String): Boolean =
        localMediaRepository.claimItemForSync(assetHash)

    override suspend fun removeSyncAsset(assetHash: String) =
        localMediaRepository.removeSyncAsset(assetHash)

    override suspend fun addSyncFailure(assetHash: String, errorMessage: String?) =
        localMediaRepository.addSyncFailure(assetHash, errorMessage)

    override suspend fun clearNonFailedSyncLogs() =
        localMediaRepository.clearNonFailedSyncLogs()
}