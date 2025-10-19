package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
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
        localAndRemoteMediaRepository.syncMedia(mediaItem.localUuid)
    }

    override fun CoroutineScope.syncJob(localItems: List<MediaItem>) {
        launch {
            localAndRemoteMediaRepository.syncMediaBatchWithPriority(
                localItems.map { it.localUuid })
        }
    }
}