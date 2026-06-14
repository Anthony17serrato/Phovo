package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemMetadataEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.ProcessingMediaEntity
import com.serratocreations.phovo.core.database.entities.ProcessingState
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.data.photos.mappers.toMediaItemWithMetadataEntity
import com.serratocreations.phovo.data.photos.mappers.toMediaItems
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

// TODO Repository APIs should not expose DAO data models
interface LocalMediaRepository: MediaRepository {
    suspend fun getMediaItemByAssetHash(assetHash: String): MediaItemWithMetadata?
    suspend fun getLocalMediaItemWithMetadataByAssetHash(assetHash: String): LocalMediaItemWithMetadata?
    suspend fun getLocalMediaByAssetHash(assetHash: String): LocalMediaEntity?
    suspend fun doesCompleteAssetExist(assetHash: String): Boolean
    suspend fun observeFirstUnprocessedFullLocalMedia(): Flow<LocalMediaEntity?>
    suspend fun tryProcessingClaim(assetHash: String): Boolean
    suspend fun removeProcessingClaim(assetHash: String)
    suspend fun addOrUpdateMediaItem(mediaItem: MediaItem)
    // TODO Repository APIs should not expose DAO data models
    suspend fun addOrUpdateLocalMediaItem(localMediaEntity: LocalMediaEntity)
    fun observeUnsyncedMediaCount(): Flow<Int>
    suspend fun getUnsyncedMediaCount(): Int
    suspend fun updateMediaItem(mediaItemMetadataEntity: MediaItemMetadataEntity)
    suspend fun getNextUnsyncedItemExcludingUuidSet(
        mediaType: MediaType
    ): LocalMediaItemWithMetadata?

    suspend fun markAsSynced(assetHash: String)

    /**
     * Thread safe API which allows any worker JOB to claim an
     * asset for synchronization. By using this API to claim assets it
     * guarantees that multiple workers do not synchronize the same asset.
     * @param assetHash the unique asset identifier
     * for the asset which is being claimed for synchronization.
     * @return A [Boolean] which indicates whether the API caller
     * is allowed to synchronize the asset. If false another worker has
     * already claimed the asset.
     */
    suspend fun claimItemForSync(assetHash: String): Boolean

    suspend fun removeSyncAsset(assetHash: String)
    suspend fun addSyncFailure(assetHash: String, errorMessage: String?)
    suspend fun clearNonFailedSyncLogs()
}

class LocalMediaRepositoryImpl(
    private val localMediaDataSource: PhovoMediaDao,
    logger: PhovoLogger
) : LocalMediaRepository {
    private val log = logger.withTag("LocalMediaRepositoryImpl")

    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        return localMediaDataSource
            .observeAllDescendingTimestamp()
            .toMediaItems()
    }

    override suspend fun getMediaItemByAssetHash(assetHash: String) =
        localMediaDataSource.getMediaItemByAssetHash(assetHash)

    override suspend fun getLocalMediaItemWithMetadataByAssetHash(assetHash: String): LocalMediaItemWithMetadata? {
        return localMediaDataSource.getLocalMediaItemWithMetadataByAssetHash(assetHash)
    }

    override suspend fun getLocalMediaByAssetHash(assetHash: String): LocalMediaEntity? {
        return localMediaDataSource.getLocalMediaByAssetHash(assetHash)
    }

    override suspend fun addOrUpdateMediaItem(mediaItem: MediaItem) {
        val (mediaItemMetadata, mediaItemLocation) = mediaItem.toMediaItemWithMetadataEntity()
        mediaItemLocation?.let { mediaItemLocationNotNull ->
            localMediaDataSource.upsertMetadataWithLocalEntity(mediaItemMetadata, mediaItemLocationNotNull)
        } ?: localMediaDataSource.upsertMetadata(mediaItemMetadata)
    }

    override suspend fun addOrUpdateLocalMediaItem(localMediaEntity: LocalMediaEntity) {
        localMediaDataSource.upsertLocal(localMediaEntity)
    }

    override suspend fun doesCompleteAssetExist(assetHash: String): Boolean {
        val asset = localMediaDataSource.getNonPartialLocalMediaByAssetHash(assetHash)
        return (asset != null)
    }

    override suspend fun observeFirstUnprocessedFullLocalMedia(): Flow<LocalMediaEntity?> =
        localMediaDataSource.observeFirstUnprocessedFullLocalMedia()

    override suspend fun tryProcessingClaim(assetHash: String): Boolean {
        val result = localMediaDataSource.tryClaim(ProcessingMediaEntity(assetHash, ProcessingState.Processing))
        return result != -1L
    }

    override suspend fun removeProcessingClaim(assetHash: String) {
        localMediaDataSource.removeClaim(assetHash = assetHash)
    }

    override fun observeUnsyncedMediaCount(): Flow<Int> =
        localMediaDataSource.observeUnsyncedMediaItemCount()

    override suspend fun updateMediaItem(mediaItemMetadataEntity: MediaItemMetadataEntity) {
        localMediaDataSource.update(mediaItemMetadataEntity)
    }

    override suspend fun getNextUnsyncedItemExcludingUuidSet(
        mediaType: MediaType
    ): LocalMediaItemWithMetadata? {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return localMediaDataSource.getNextUnsyncedLocalItemExcludingSet(
            mediaType = mediaType,
            currentTimeUtcMs = currentTime
        )
    }

    override suspend fun claimItemForSync(assetHash: String): Boolean {
        return localMediaDataSource.claimItemForSync(assetHash)
    }

    override suspend fun removeSyncAsset(assetHash: String) =
        localMediaDataSource.removeSyncAsset(assetHash)

    override suspend fun addSyncFailure(assetHash: String, errorMessage: String?) {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        localMediaDataSource.addSyncFailure(assetHash, errorMessage, currentTime)
    }

    override suspend fun clearNonFailedSyncLogs() {
        localMediaDataSource.clearInProgressSyncLogs()
    }

    override suspend fun markAsSynced(assetHash: String) {
        localMediaDataSource.markAsSynced(assetHash)
    }

    override suspend fun getUnsyncedMediaCount(): Int = observeUnsyncedMediaCount().first()
}