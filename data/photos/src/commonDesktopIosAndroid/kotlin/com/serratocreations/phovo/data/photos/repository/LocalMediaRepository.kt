package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.MediaItemMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.data.photos.mappers.toMediaItemWithUriEntity
import com.serratocreations.phovo.data.photos.mappers.toMediaItems
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface LocalMediaRepository: MediaRepository {
    suspend fun getMediaItemByLocalUuid(uuid: String): MediaItemWithMetadata?
    suspend fun addOrUpdateMediaItem(mediaItem: MediaItem)
    suspend fun addOrUpdateMediaItem(mediaItemWithMetadata: MediaItemWithMetadata)
    fun observeUnsyncedMediaCount(): Flow<Int>
    suspend fun getUnsyncedMediaCount(): Int
    suspend fun updateMediaItem(mediaItemMetadata: MediaItemMetadata)
    suspend fun getNextUnsyncedItemExcludingUuidSet(
        syncInProgressSet: Set<String>,
        mediaType: MediaType
    ): MediaItemWithMetadata?
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

    override suspend fun getMediaItemByLocalUuid(uuid: String) =
        localMediaDataSource.getMediaItemByLocalUuid(uuid)

    override suspend fun addOrUpdateMediaItem(mediaItem: MediaItem) {
        addOrUpdateMediaItem(mediaItem.toMediaItemWithUriEntity())
    }

    override suspend fun addOrUpdateMediaItem(mediaItemWithMetadata: MediaItemWithMetadata) {
        val (mediaItemMetadata, mediaItemLocation) = mediaItemWithMetadata
        localMediaDataSource.insert(mediaItemMetadata, mediaItemLocation)
    }

    override fun observeUnsyncedMediaCount(): Flow<Int> =
        localMediaDataSource.observeUnsyncedMediaItemCount()

    override suspend fun updateMediaItem(mediaItemMetadata: MediaItemMetadata) {
        localMediaDataSource.update(mediaItemMetadata)
    }

    override suspend fun getNextUnsyncedItemExcludingUuidSet(
        syncInProgressSet: Set<String>,
        mediaType: MediaType
    ): MediaItemWithMetadata? {
        return localMediaDataSource.getNextUnsyncedItemExcludingUuidSet(
            excludingHashes = syncInProgressSet,
            mediaType = mediaType,
            excludeNotEmpty = syncInProgressSet.isNotEmpty()
        )
    }

    override suspend fun getUnsyncedMediaCount(): Int = observeUnsyncedMediaCount().first()
}