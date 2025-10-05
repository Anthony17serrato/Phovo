package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.mappers.toMediaItem
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.extensions.toMediaItemEntity
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

open class LocalSupportMediaRepository(
    private val localMediaDataSource: PhovoMediaDao,
    remoteMediaDataSource: MediaNetworkDataSource,
    logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher
) : MediaRepository(
    remotePhotosDataSource = remoteMediaDataSource
) {
    private val log = logger.withTag("LocalSupportMediaRepositoryImpl")

    // TODO: Implement paging
    final override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        val remoteItemsFlow = super.phovoMediaFlow()
        val localItemsFlow = localMediaDataSource
            .observeAllDescendingTimestamp()
            .toMediaItems()

        return combine(remoteItemsFlow, localItemsFlow) { remote, local ->
            (local + remote).distinctBy { it.localUuid }
        }.flowOn(ioDispatcher)
    }

    // TODO Map to media item when model URI uses a common solution
    suspend fun getMediaItemByLocalUuid(uuid: String) =
        localMediaDataSource.getMediaItemByLocalUuid(uuid)

    suspend fun addOrUpdateMediaItem(mediaItem: MediaItem) {
        localMediaDataSource.insert(mediaItem.toMediaItemEntity())
    }

    private fun Flow<List<MediaItemEntity>>.toMediaItems(): Flow<List<MediaItem>> =
        map { localItems ->
            localItems.map {
                it.toMediaItem()
            }
        }
}