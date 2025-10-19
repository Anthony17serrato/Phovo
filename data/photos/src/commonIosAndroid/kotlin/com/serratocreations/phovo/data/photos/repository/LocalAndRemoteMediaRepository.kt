package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.mappers.toMediaItemDto
import com.serratocreations.phovo.data.photos.mappers.toMediaItemEntity
import com.serratocreations.phovo.data.photos.network.model.SyncSuccessful
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

interface LocalAndRemoteMediaRepository: MediaRepository {
    /**
     * Adds the [mediaLocalUuid] to the sync queue, media added using this API gets picked up by
     * sync workers in a first in first out fashion. In the event that a large amount of items have been
     * enqueued for sync additional requests may be dropped. For high priority sync use the
     * [syncMediaBatchWithPriority] API
     */
    fun syncMedia(mediaLocalUuid: String)

    /**
     * Prioritizes the provided batch of UUIDs for sync.
     * Using this API to sync media gives immediate priority to the sync request versus the
     * [syncMedia] API. This API should typically be reserved for periodic sync work requests or
     * user initiated sync requests.
     * @param mediaLocalUuids the set of string uuids to sync with priority
     */
    suspend fun syncMediaBatchWithPriority(mediaLocalUuids: List<String>)
}

class LocalAndRemoteMediaRepositoryImpl(
    private val localMediaRepository: LocalMediaRepository,
    private val remoteMediaRepository: RemoteMediaRepository,
    applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
): LocalAndRemoteMediaRepository {
    companion object {
        private const val SYNC_WORKER_COUNT = 8
        private const val BUFFER_LIMIT = 10_000
    }

    // prevent large amounts of sync workers running in parallel
    private val limitedParallelIoDispatcher = ioDispatcher.limitedParallelism(SYNC_WORKER_COUNT)

    // Configures sync request channel with a reasonable buffer, if the buffer is exceeded additional
    // items will be picked up by the periodic sync worker.
    private val syncRequestChannel = Channel<String>(capacity = BUFFER_LIMIT, onBufferOverflow = BufferOverflow.DROP_LATEST)

    init {
        repeat(SYNC_WORKER_COUNT) {
            applicationScope.syncWorker(syncRequestChannel)
        }
    }

    private fun CoroutineScope.syncWorker(syncReceiveChannel: ReceiveChannel<String>) = launch {
        for (uuid in syncReceiveChannel) {
            yield()
            // TODO check uuid is not being processed by other workers and add to set of uuids
            //  which are being currently processed
            val mediaItemEntity = localMediaRepository.getMediaItemByLocalUuid(uuid)
            mediaItemEntity?.let { mediaItemEntityNotNull ->
                val result = remoteMediaRepository.syncMedia(
                    media = mediaItemEntityNotNull.toMediaItemDto(),
                    mediaUri = mediaItemEntityNotNull.mediaItemUri.uri
                )
                if (result is SyncSuccessful) {
                    localMediaRepository.updateMediaItem(
                        result.updatedMediaItemDto.toMediaItemEntity())
                }
            }
        }
    }

    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        val remoteItemsFlow = remoteMediaRepository.phovoMediaFlow()
        val localItemsFlow = localMediaRepository.phovoMediaFlow()

        return combine(remoteItemsFlow, localItemsFlow) { remote, local ->
            (local + remote).distinctBy { it.localUuid }
        }.flowOn(ioDispatcher)
    }

    override fun syncMedia(mediaLocalUuid: String) {
        syncRequestChannel.trySend(mediaLocalUuid)
    }

    override suspend fun syncMediaBatchWithPriority(mediaLocalUuids: List<String>) {
        coroutineScope {
            withContext(limitedParallelIoDispatcher) {
                val priorityChannel = Channel<String>(Channel.RENDEZVOUS)
                repeat(SYNC_WORKER_COUNT) {
                    syncWorker(priorityChannel)
                }
                mediaLocalUuids.forEach { uuid ->
                    priorityChannel.send(uuid)
                }
                priorityChannel.close()
                // Suspends until all workers complete
            }
        }
    }
}