package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.repository.model.SyncImage
import com.serratocreations.phovo.data.photos.repository.model.SyncVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IosAndroidLocalMediaManager(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    localMediaRepository: LocalMediaRepository,
    localMediaProcessor: LocalMediaProcessor,
    appScope: CoroutineScope,
    logger: PhovoLogger,
): LocalMediaManager(
    localMediaRepository,
    localMediaProcessor,
    appScope,
    logger
) {
    override suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        super.handleProcessedMediaItem(mediaItem)
        val syncQueueable = when(mediaItem) {
            is MediaImageItem -> SyncImage(mediaItem.localUuid)
            is MediaVideoItem -> SyncVideo(mediaItem.localUuid)
        }
        localAndRemoteMediaRepository.syncMedia(syncQueueable)
    }

    override fun CoroutineScope.syncJob(localItems: List<MediaItem>) {
        launch {
            val syncQueueables = localItems.map {
                when(it) {
                    is MediaImageItem -> SyncImage(it.localUuid)
                    is MediaVideoItem -> SyncVideo(it.localUuid)
                }
            }
            localAndRemoteMediaRepository.syncMediaBatchWithPriority(syncQueueables)
        }
    }
}