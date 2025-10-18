package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.mappers.toMediaItemDto
import com.serratocreations.phovo.data.photos.mappers.toMediaItemEntity
import com.serratocreations.phovo.data.photos.network.model.SyncSuccessful
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn

interface LocalAndRemoteMediaRepository: MediaRepository {
    suspend fun syncMedia()
}

class LocalAndRemoteMediaRepositoryImpl(
    private val localMediaRepository: LocalMediaRepository,
    private val remoteMediaRepository: RemoteMediaRepository,
    private val ioDispatcher: CoroutineDispatcher
): LocalAndRemoteMediaRepository {

    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        val remoteItemsFlow = remoteMediaRepository.phovoMediaFlow()
        val localItemsFlow = localMediaRepository.phovoMediaFlow()

        return combine(remoteItemsFlow, localItemsFlow) { remote, local ->
            (local + remote).distinctBy { it.localUuid }
        }.flowOn(ioDispatcher)
    }

    override suspend fun syncMedia() {
        // TODO: create a queue of in flight sync jobs so that new requests only trigger sync for media
        //  which is not already queued(or something similarly optimized)
        val unsyncedData = localMediaRepository.observeUnsyncedMedia().first()
        unsyncedData.forEach { mediaItemEntity ->
            val result = remoteMediaRepository.syncMedia(
                media = mediaItemEntity.toMediaItemDto(),
                mediaUri = mediaItemEntity.mediaItemUri.uri
            )
            if (result is SyncSuccessful) {
                localMediaRepository.updateMediaItem(
                    result.updatedMediaItemDto.toMediaItemEntity())
            }
        }
    }
}