package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

class IosAndroidLocalSupportMediaRepository(
    localMediaDataSource: PhovoMediaDao,
    private val remoteMediaDataSource: MediaNetworkDataSource,
    localMediaProcessor: LocalMediaProcessor,
    logger: PhovoLogger,
    appScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher
): CommonLocalSupportMediaRepository(
    localMediaDataSource = localMediaDataSource,
    remoteMediaDataSource = remoteMediaDataSource,
    localMediaProcessor = localMediaProcessor,
    logger = logger,
    appScope = appScope,
    ioDispatcher = ioDispatcher
) {
    override suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        super.handleProcessedMediaItem(mediaItem)
        remoteMediaDataSource.syncMedia(mediaItem)
    }
}