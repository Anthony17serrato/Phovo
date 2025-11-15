package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithUriEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.data.photos.mappers.toMediaItemWithUriEntity
import com.serratocreations.phovo.data.photos.mappers.toMediaItems
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface LocalMediaRepository: MediaRepository {
    suspend fun getMediaItemByLocalUuid(uuid: String): MediaItemWithUriEntity?
    suspend fun addOrUpdateMediaItem(mediaItem: MediaItem)
    suspend fun addOrUpdateMediaItem(mediaItemWithUriEntity: MediaItemWithUriEntity)
    fun observeUnsyncedMedia():  Flow<List<MediaItem>>
    fun observeUnsyncedMediaCount(): Flow<Int>
    suspend fun getUnsyncedMediaCount(): Int
    suspend fun updateMediaItem(mediaItemEntity: MediaItemEntity)
    suspend fun getNextUnsyncedItemExcludingUuidSet(
        syncInProgressSet: Set<String>,
        mediaType: MediaType
    ): MediaItemWithUriEntity?
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

    // TODO Map to media item when model URI uses a common solution(use filekit)
    override suspend fun getMediaItemByLocalUuid(uuid: String) =
        localMediaDataSource.getMediaItemByLocalUuid(uuid)

    override suspend fun addOrUpdateMediaItem(mediaItem: MediaItem) {
        addOrUpdateMediaItem(mediaItem.toMediaItemWithUriEntity())
    }

    override suspend fun addOrUpdateMediaItem(mediaItemWithUriEntity: MediaItemWithUriEntity) {
        val (mediaItemEntity, mediaItemUriEntity) = mediaItemWithUriEntity
        localMediaDataSource.insert(mediaItemEntity, mediaItemUriEntity)
    }

    override suspend fun updateMediaItem(mediaItemEntity: MediaItemEntity) {
        localMediaDataSource.update(mediaItemEntity)
    }

    override suspend fun getNextUnsyncedItemExcludingUuidSet(
        syncInProgressSet: Set<String>,
        mediaType: MediaType
    ): MediaItemWithUriEntity? {
        return localMediaDataSource.getNextUnsyncedItemExcludingUuidSet(
            excludingUuids = syncInProgressSet,
            mediaType = mediaType,
            excludeNotEmpty = syncInProgressSet.isNotEmpty()
        )
    }

    override fun observeUnsyncedMedia(): Flow<List<MediaItem>> =
        localMediaDataSource.observeAllUnsyncedMediaItems().toMediaItems()

    override fun observeUnsyncedMediaCount(): Flow<Int> =
        localMediaDataSource.observeUnsyncedMediaItemCount()

    override suspend fun getUnsyncedMediaCount(): Int = observeUnsyncedMediaCount().first()
}