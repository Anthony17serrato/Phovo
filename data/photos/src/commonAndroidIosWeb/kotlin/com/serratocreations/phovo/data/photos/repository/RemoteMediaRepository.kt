package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.serverconfig.IosAndroidWasmServerConfigRepository
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

class RemoteMediaRepositoryImpl(
    private val remotePhotosDataSource: MediaNetworkDataSource,
    private val serverConfigRepository: IosAndroidWasmServerConfigRepository,
    logger: PhovoLogger
): RemoteMediaRepository {
    private val log = logger.withTag("RemoteMediaRepositoryImpl")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        return serverConfigRepository.observeServerConfig().flatMapLatest {
            it?.serverBaseUrlString?.let { serverUrlNotNull ->
                remotePhotosDataSource.allItemsFlow(serverUrlNotNull)
                    .onStart { emit(emptyList()) }
            } ?: flowOf(emptyList())
        }
    }

    override suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String
    ): SyncResult {
        val baseUrl = serverConfigRepository.observeServerConfig().first()?.serverBaseUrlString
        if (baseUrl == null) {
            log.i { "syncMedia failed because baseUrl is null" }
            return SyncResult.SyncError
        }

        return remotePhotosDataSource.syncMedia(
            mediaItemDto = media,
            mediaUri = mediaUri,
            baseUrl = baseUrl
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeServerConnection(): Flow<Boolean> {
        return serverConfigRepository.observeServerConfig().flatMapLatest {
            flow {
                if (it?.serverBaseUrlString == null) {
                    emit(false)
                } else {
                    while(currentCoroutineContext().isActive) {
                        yield()
                        emit(remotePhotosDataSource.checkServerConnection(it.serverBaseUrlString))
                        delay(30.seconds)
                    }
                }
            }
        }
    }
}