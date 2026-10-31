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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

interface LocalAndRemoteMediaRepository: LocalMediaRepository, RemoteMediaRepository {

    /**
     * Adds the [syncQueueable] to the sync queue. media added using this API gets picked up by
     * sync workers in a first in first out fashion.
     */
    suspend fun syncMedia(syncQueueable: SyncQueueable)

    /**
     * Runs an application wide sync while the returned flow is collected. Nothing runs until
     * collection starts, and cancelling the collector cancels the sync. This API should typically be
     * initiated by a work manager(or equivalent platform API)
     *
     * Emits [SyncState.Syncing] as soon as the sync starts and again as it progresses, then a single
     * [SyncState.SyncComplete] once every worker has finished, and completes. Progress belongs to
     * this collection: counts start from zero and leave out uploads queued with [syncMedia].
     *
     * Only one sync runs at a time. Collecting while another sync is running waits for it to finish
     * before emitting anything.
     *
     * @param scanJob a reference to the Job which is scanning for new media, while the scan job is
     * active sync workers will remain running, so media it processes during the sync is picked up.
     */
    fun sync(scanJob: Job): Flow<SyncState>
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
        syncProgressStateFlow: MutableStateFlow<SyncState.Syncing>
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
            val result = syncMediaItem(nextUnsyncedItem, syncProgressStateFlow)
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
                syncMediaItem(mediaItemEntityNotNull)
            }
        }
    }

    /**
     * Uploads one item and marks it synced when the upload succeeds.
     *
     * @param syncProgressStateFlow progress of the sync this upload belongs to. It tracks the bytes
     *   sent while uploading, and credits the item once it succeeds. Null for uploads that aren't
     *   part of a sync, such as ones queued with [syncMedia].
     */
    private suspend fun syncMediaItem(
        mediaItemEntity: LocalMediaItemWithMetadata,
        syncProgressStateFlow: MutableStateFlow<SyncState.Syncing>? = null
    ): NetworkResult<Unit> {
        val metadata = mediaItemEntity.mediaItemMetadataEntity
        val assetHash = metadata.assetHash
        val result = remoteMediaRepository.syncMedia(
            media = metadata.toMediaItemDto(),
            mediaUri = mediaItemEntity.localLocation.localUri,
            onBytesSent = { bytesSent ->
                // Capped at the recorded size, so a file whose stored size is out of date
                // can't push completed past total.
                syncProgressStateFlow?.update { currentState ->
                    currentState.copy(
                        inFlightBytes = currentState.inFlightBytes +
                            (assetHash to bytesSent.coerceAtMost(metadata.size))
                    )
                }
            }
        )
        val isSuccess = result is NetworkResult.NetworkSuccess
        // Credited before marking synced, so the total never drops ahead of completed.
        syncProgressStateFlow?.update {
            it.copy(
                inFlightBytes = it.inFlightBytes - assetHash,
                syncedCount = if (isSuccess) it.syncedCount + 1 else it.syncedCount,
                syncedBytes = if (isSuccess) it.syncedBytes + metadata.size else it.syncedBytes
            )
        }
        if (isSuccess) {
            localMediaRepository.markAsSynced(assetHash = assetHash)
        } else {
            log.w { "sync failed to upload item with hash $assetHash with result $result" }
        }
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

    // Held for the whole sync, so only one runs at a time. Each sync starts by clearing the claims in
    // the sync log, so a second sync running alongside would release the first one's claims and
    // upload the same items again.
    private val syncMutex = Mutex()
    override fun sync(scanJob: Job): Flow<SyncState> = channelFlow {
        syncMutex.withLock {
            clearNonFailedSyncLogs()
            val initialUnsynced = observeUnsyncedMedia().first()
            val syncState = MutableStateFlow(
                SyncState.Syncing(
                    currentPendingSyncQuantity = initialUnsynced.count,
                    unsyncedBytes = initialUnsynced.bytes
                )
            )

            val syncProgressJob = launch {
                launch {
                    observeUnsyncedMedia().collect { unsyncedMedia ->
                        syncState.update { currentState ->
                            currentState.copy(
                                currentPendingSyncQuantity = unsyncedMedia.count,
                                unsyncedBytes = unsyncedMedia.bytes
                            )
                        }
                    }
                }
                // Forward progress to the collector. Updates that arrive faster than it reads them
                // are conflated, so it gets the latest.
                syncState.collect {
                    send(it)
                }
            }

            val syncJobs = List(SYNC_IMAGE_WORKER_COUNT) {
                syncWorker(MediaType.Image, scanJob, syncState)
            } + List(SYNC_VIDEO_WORKER_COUNT) {
                syncWorker(MediaType.Video, scanJob, syncState)
            }
            syncJobs.joinAll()
            syncProgressJob.cancelAndJoin()

            send(
                SyncState.SyncComplete(
                    backedUpQuantity = syncState.value.syncedCount,
                    failureQuantity = observeUnsyncedMedia().first().count
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