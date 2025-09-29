package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.network.IosAndroidMediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher

class IosAndroidLocalSupportMediaRepository(
    localMediaDataSource: PhovoMediaDao,
    private val remoteMediaDataSource: IosAndroidMediaNetworkDataSource,
    logger: PhovoLogger,
    ioDispatcher: CoroutineDispatcher
): LocalSupportMediaRepository(
    localMediaDataSource = localMediaDataSource,
    remoteMediaDataSource = remoteMediaDataSource,
    logger = logger,
    ioDispatcher = ioDispatcher
) {
    suspend fun syncMedia(mediaItem: MediaItem) {
        // TODO: Perhaps it is better to query for unsynced media instead of
        //  receiving the media directly
        remoteMediaDataSource.syncMedia(mediaItem)
    }
}