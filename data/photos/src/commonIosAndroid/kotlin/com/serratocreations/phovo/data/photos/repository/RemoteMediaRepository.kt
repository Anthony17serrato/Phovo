package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

class RemoteMediaRepositoryImpl(
    private val remotePhotosDataSource: MediaNetworkDataSource,
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    applicationScope: CoroutineScope,
    logger: PhovoLogger
): RemoteMediaRepository {
    private val log = logger.withTag("RemoteMediaRepositoryImpl")

    companion object {
        private val CHECK_ALIVE_DELAY = 15.seconds
    }

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
    ): NetworkResult<Unit> {
        val baseUrl = serverConfigRepository.observeServerConfig().first()?.serverBaseUrlString
        if (baseUrl == null) {
            val errorMessage = "syncMedia failed because baseUrl is null"
            log.i { errorMessage }
            return NetworkResult.NetworkError(errorMessage)
        }

        return remotePhotosDataSource.syncMedia(
            mediaItemDto = media,
            mediaUri = mediaUri,
            baseUrl = baseUrl,
            retryPolicy = NetworkCallRetryPolicy.RetryAfterLambda {
                // drop current state to ensure cached connection status is not used
                isSeverConnected.drop(1).filter { it }.first()
            }
        )
    }

    // TODO: Most likely there is a more sophisticated networking method to check alive
    //  then pinging every X seconds(Investigate)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val isSeverConnected = serverConfigRepository.observeServerConfig().flatMapLatest {
        flow {
            if (it?.serverBaseUrlString == null) {
                emit(false)
            } else {
                while(currentCoroutineContext().isActive) {
                    yield()
                    emit(remotePhotosDataSource.checkServerConnection(it.serverBaseUrlString))
                    delay(CHECK_ALIVE_DELAY)
                }
            }
        }
    }.shareIn(
        scope = applicationScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    override fun observeServerConnection(): Flow<Boolean> {
        return isSeverConnected
    }
}