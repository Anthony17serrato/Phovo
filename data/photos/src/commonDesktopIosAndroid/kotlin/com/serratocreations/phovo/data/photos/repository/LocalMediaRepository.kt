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
        syncInProgressSet: Set<String>,
        mediaType: MediaType
    ): LocalMediaItemWithMetadata?

    suspend fun markAsSynced(assetHash: String)
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
        syncInProgressSet: Set<String>,
        mediaType: MediaType
    ): LocalMediaItemWithMetadata? {
        return localMediaDataSource.getNextUnsyncedLocalItemExcludingSet(
            excludingHashes = syncInProgressSet,
            mediaType = mediaType,
            excludeNotEmpty = syncInProgressSet.isNotEmpty()
        )
    }

    override suspend fun markAsSynced(assetHash: String) {
        localMediaDataSource.markAsSynced(assetHash)
    }

    override suspend fun getUnsyncedMediaCount(): Int = observeUnsyncedMediaCount().first()
}