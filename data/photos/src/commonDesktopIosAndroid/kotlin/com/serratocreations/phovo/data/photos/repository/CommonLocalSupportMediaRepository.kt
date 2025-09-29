package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.PhovoMediaEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.extensions.toMediaItem
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.extensions.toPhovoMediaEntity
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
            (local + remote).distinctBy { it.uri }
        }.flowOn(ioDispatcher)
    }

    suspend fun addMediaItem(mediaItem: MediaItem) {
        localMediaDataSource.insert(mediaItem.toPhovoMediaEntity())
    }

    private fun Flow<List<PhovoMediaEntity>>.toMediaItems(): Flow<List<MediaItem>> =
        map { localItems ->
            localItems.map {
                it.toMediaItem()
            }
        }
}