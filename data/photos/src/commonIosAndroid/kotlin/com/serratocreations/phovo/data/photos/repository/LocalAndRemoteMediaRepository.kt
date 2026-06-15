package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemMetadataEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.LocalMediaBackupProgress
import com.serratocreations.phovo.data.photos.mappers.toMediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.NetworkResult
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.SyncImage
import com.serratocreations.phovo.data.photos.repository.model.SyncQueueable
import com.serratocreations.phovo.data.photos.repository.model.SyncVideo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

interface LocalAndRemoteMediaRepository: LocalMediaRepository, RemoteMediaRepository {
    val syncProgressState: StateFlow<LocalMediaBackupProgress>
    /**
     * Adds the [syncQueueable] to the sync queue. media added using this API gets picked up by
     * sync workers in a first in first out fashion.
     */
    suspend fun syncMedia(syncQueueable: SyncQueueable)

    /**
     * Initiates an application wide sync job, when this API is called multiple times and there is
     * already a sync job running, subsequent calls are dropped and the existing Job object is returned.
     * This API should typically be initiated by a work manager(or equivalent platform API)
     *
     * @param processingJob a reference to the Job which is scanning for new media, while the scan job is
     * active sync workers will remain running.
     * @return a [Deferred] of type [Job]. The deferred is guaranteed to complete promptly as it is only
     * used to avoid a locking algorithm implementation. The Job will allow callers to suspend until
     * the sync completes(Useful for periodic sync workers which must wrap the Job)
     */
    fun initiateSyncJob(
        processingJob: Job
    ): Deferred<Job>
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
    // Since only one sync job should be running at any time, a single thread dispatcher can be used
    // to avoid a locking algorithm
    private val singleThreadDefaultDispatcher = defaultDispatcher.limitedParallelism(parallelism = 1)
    // Only one job should be active, this property should only be accessed from the single thread dispatcher
    //  to avoid any parallelism issues
    private var syncJob: Job? = null

    private val _syncProgressState = MutableStateFlow(LocalMediaBackupProgress())
    override val syncProgressState = _syncProgressState.asStateFlow()

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
            val isAddedToSync = localMediaRepository.claimItemForSync(syncCandidate.mediaItemMetadataEntity.assetHash)
        } while (isAddedToSync.not())
        return syncCandidate
    }

    private fun CoroutineScope.syncWorker(
        type: MediaType,
        processingJob: Job
    ) = launch {
        while (this.isActive) {
            val nextUnsyncedItem = getNextUnsyncedItem(type) ?: run {
                // Keep worker running if processing job has not completed
                if (processingJob.isActive) {
                    delay(WORKER_IDLE_DELAY)
                    continue
                } else {
                    return@launch
                }
            }
            log.i { "claimed item hash ${nextUnsyncedItem.mediaItemMetadataEntity.assetHash}" }
            // Do not sync if server is not connected
            remoteMediaRepository.observeServerConnection().filter { it }.first()
            val assetHash = nextUnsyncedItem.mediaItemMetadataEntity.assetHash
            log.i { "starting sync for item hash $assetHash" }
            // Terminate the worker if there is no remaining items to sync
            val result = sync(nextUnsyncedItem)
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
            if (result is NetworkResult.NetworkSuccess) {
                _syncProgressState.update { currentState ->
                    currentState.copy(syncedCount = (currentState.syncedCount + 1))
                }
            }
        }
    }

    private fun CoroutineScope.syncWorker(syncReceiveChannel: ReceiveChannel<String>) = launch {
        for (assetHash in syncReceiveChannel) {
            yield()
            // TODO check assetHash is not being processed by other workers and add to set of uuids
            //  which are being currently processed
            val localMediaItemWithMetadata = localMediaRepository.getLocalMediaItemWithMetadataByAssetHash(assetHash)
            localMediaItemWithMetadata?.let { mediaItemEntityNotNull ->
                sync(mediaItemEntityNotNull)
            }
        }
    }

    private suspend fun sync(mediaItemEntity: LocalMediaItemWithMetadata): NetworkResult<Unit> {
        val result = remoteMediaRepository.syncMedia(
            media = mediaItemEntity.mediaItemMetadataEntity.toMediaItemDto(),
            mediaUri = mediaItemEntity.localLocation.localUri
        )
        log.i { "sync complete result $result hash ${mediaItemEntity.mediaItemMetadataEntity.assetHash}" }
        if (result is NetworkResult.NetworkSuccess) {
            localMediaRepository.markAsSynced(
                assetHash = mediaItemEntity.mediaItemMetadataEntity.assetHash
            )
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
        when(syncQueueable) {
            is SyncImage -> syncImageRequestChannel.send(syncQueueable.uuid)
            is SyncVideo -> syncVideoRequestChannel.send(syncQueueable.uuid)
        }
    }

    private suspend fun initiateSyncJobInternal(processingJob: Job) = coroutineScope {
        val initialUnsyncedCount = localMediaRepository.getUnsyncedMediaCount()
        _syncProgressState.update { currentProgress ->
            LocalMediaBackupProgress(currentPendingSyncQuantity = initialUnsyncedCount)
        }

        val pendingSyncCountObservationJob = observeUnsyncedMediaCount()
            .onEach { unsyncedCount ->
                _syncProgressState.update { currentProgress ->
                    currentProgress.copy(currentPendingSyncQuantity = unsyncedCount)
                }
            }.launchIn(this)
        val syncJobs = mutableListOf<Job>()
        repeat(SYNC_IMAGE_WORKER_COUNT) {
            syncJobs.add(syncWorker(MediaType.Image, processingJob))
        }
        repeat(SYNC_VIDEO_WORKER_COUNT) {
            syncJobs.add(syncWorker(MediaType.Video, processingJob))
        }
        joinAll(*syncJobs.toTypedArray())
        pendingSyncCountObservationJob.cancel()

        val finalUnsyncedCount = localMediaRepository.getUnsyncedMediaCount()
        _syncProgressState.update { currentProgress ->
            currentProgress.copy(
                currentPendingSyncQuantity = finalUnsyncedCount,
                isSyncComplete = true
            )
        }
    }

    override fun initiateSyncJob(
        processingJob: Job
    ): Deferred<Job> =
        applicationScope.async(singleThreadDefaultDispatcher) {
            syncJob?.let { syncJobNotNull ->
                if (syncJobNotNull.isActive) {
                    return@async syncJobNotNull
                }
            }
            // At this point we have validated that no sync Job is actively running
            syncJob?.cancel()
            // Job can be launched with full parallelism support since the need for thread safety has passed
            return@async launch(defaultDispatcher) {
                initiateSyncJobInternal(processingJob)
            }.apply {
                syncJob = this
            }
        }

    override suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String
    ) = remoteMediaRepository.syncMedia(media, mediaUri)

    override fun observeServerConnection() = remoteMediaRepository.observeServerConnection()

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

    override fun observeUnsyncedMediaCount() = localMediaRepository.observeUnsyncedMediaCount()

    override suspend fun getUnsyncedMediaCount() = localMediaRepository.getUnsyncedMediaCount()

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