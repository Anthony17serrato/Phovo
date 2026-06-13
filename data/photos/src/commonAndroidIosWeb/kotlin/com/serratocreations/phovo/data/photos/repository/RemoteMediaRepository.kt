package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
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
    private val serverConfigRepository: ServerConfigRepository,
    logger: PhovoLogger
): RemoteMediaRepository {
    private val log = logger.withTag("RemoteMediaRepositoryImpl")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        return serverConfigRepository.observeServerConfig().flatMapLatest { config ->
            val clientConfig = config as? ServerConfig.ClientSpecificServerConfig
            clientConfig?.serverBaseUrlString?.let { serverUrlNotNull ->
                remotePhotosDataSource.allItemsFlow(serverUrlNotNull)
                    .onStart { emit(emptyList()) }
            } ?: flowOf(emptyList())
        }
    }

    override suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String
    ): SyncResult {
        val clientConfig = serverConfigRepository.observeServerConfig().first() as? ServerConfig.ClientSpecificServerConfig
        val baseUrl = clientConfig?.serverBaseUrlString
        if (baseUrl == null) {
            val errorMessage = "syncMedia failed because baseUrl is null"
            log.i { errorMessage }
            return SyncResult.SyncError(errorMessage)
        }

        return remotePhotosDataSource.syncMedia(
            mediaItemDto = media,
            mediaUri = mediaUri,
            baseUrl = baseUrl
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeServerConnection(): Flow<Boolean> {
        return serverConfigRepository.observeServerConfig().flatMapLatest { config ->
            val clientConfig = config as? ServerConfig.ClientSpecificServerConfig
            flow {
                if (clientConfig?.serverBaseUrlString == null) {
                    emit(false)
                } else {
                    while(currentCoroutineContext().isActive) {
                        yield()
                        emit(remotePhotosDataSource.checkServerConnection(clientConfig.serverBaseUrlString))
                        delay(30.seconds)
                    }
                }
            }
        }
    }
}