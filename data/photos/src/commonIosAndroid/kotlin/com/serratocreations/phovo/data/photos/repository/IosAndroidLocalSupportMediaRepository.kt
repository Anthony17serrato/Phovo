package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.mappers.toMediaItemDto
import com.serratocreations.phovo.data.photos.local.mappers.toMediaItemEntity
import com.serratocreations.phovo.data.photos.network.IosAndroidMediaNetworkDataSource
import com.serratocreations.phovo.data.photos.network.SyncSuccessful
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first

class IosAndroidLocalSupportMediaRepository(
    private val localMediaDataSource: PhovoMediaDao,
    private val remoteMediaDataSource: IosAndroidMediaNetworkDataSource,
    logger: PhovoLogger,
    ioDispatcher: CoroutineDispatcher
): LocalSupportMediaRepository(
    localMediaDataSource = localMediaDataSource,
    remoteMediaDataSource = remoteMediaDataSource,
    logger = logger,
    ioDispatcher = ioDispatcher
) {
    suspend fun syncMedia() {
        // TODO: create a queue of in flight sync jobs so that new requests only trigger sync for media
        //  which is not already queued
        val unsyncedData = localMediaDataSource.observeAllUnsyncedMediaItems().first()
        unsyncedData.forEach { mediaItemEntity ->
            val result = remoteMediaDataSource.syncMedia(mediaItemEntity.toMediaItemDto())
            if (result is SyncSuccessful) {
                localMediaDataSource.insert(result.updatedMediaItemDto.toMediaItemEntity())
            }
        }
    }
}