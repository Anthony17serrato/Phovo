package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithUriEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.mappers.toMediaItemWithUriEntity
import com.serratocreations.phovo.data.photos.mappers.toMediaItems
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository: MediaRepository {
    suspend fun getMediaItemByLocalUuid(uuid: String): MediaItemWithUriEntity?
    suspend fun addOrUpdateMediaItem(mediaItem: MediaItem)
    suspend fun addOrUpdateMediaItem(mediaItemWithUriEntity: MediaItemWithUriEntity)
    fun observeUnsyncedMedia():  Flow<List<MediaItemWithUriEntity>>
    suspend fun updateMediaItem(mediaItemEntity: MediaItemEntity)
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

    // TODO Map to media item when model URI uses a common solution
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

    override fun observeUnsyncedMedia():  Flow<List<MediaItemWithUriEntity>> =
        localMediaDataSource.observeAllUnsyncedMediaItems()
}