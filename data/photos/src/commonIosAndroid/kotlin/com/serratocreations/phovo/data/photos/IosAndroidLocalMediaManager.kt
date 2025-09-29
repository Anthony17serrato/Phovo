package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.IosAndroidLocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IosAndroidLocalMediaManager(
    private val mediaRepository: IosAndroidLocalSupportMediaRepository,
    localMediaProcessor: LocalMediaProcessor,
    appScope: CoroutineScope,
    logger: PhovoLogger,
): LocalMediaManager(
    mediaRepository,
    localMediaProcessor,
    appScope,
    logger
) {
    override suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        super.handleProcessedMediaItem(mediaItem)
        mediaRepository.syncMedia(mediaItem)
    }

    override fun CoroutineScope.syncJob(localItems: List<MediaItem>) {
        launch {
            // TODO Filter out only items which have not been server synced
            localItems.forEach { mediaItem ->
                mediaRepository.syncMedia(mediaItem)
            }
        }
    }
}