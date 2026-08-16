package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerEndpointResolver
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

class RemoteMediaRepositoryImpl(
    private val remotePhotosDataSource: MediaNetworkDataSource,
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val endpointResolver: ServerEndpointResolver,
    applicationScope: CoroutineScope,
    logger: PhovoLogger
): RemoteMediaRepository {
    private val log = logger.withTag("RemoteMediaRepositoryImpl")

    companion object {
        private val CHECK_ALIVE_DELAY = 15.seconds
    }

    init {
        // TODO: Most likely there is a more sophisticated networking method to check alive
        //  then pinging every X seconds(Investigate)
        applicationScope.launch {
            // Keyed on identity, not address, so re-resolving does not restart the poll.
            serverConfigRepository.observeServerConfig().collectLatest { serverConfig ->
                if (serverConfig == null) return@collectLatest
                while (currentCoroutineContext().isActive) {
                    yield()
                    // Drop the memoised endpoint so the address is genuinely re-probed; the
                    // resolver falls through to an mDNS browse if it stopped answering.
                    endpointResolver.invalidate()
                    endpointResolver.resolve()
                    delay(CHECK_ALIVE_DELAY)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        return serverConfigRepository.observeServerConfig().flatMapLatest { serverConfig ->
            if (serverConfig == null) {
                flowOf(emptyList())
            } else {
                flow {
                    val baseUrl = endpointResolver.resolve()
                    if (baseUrl == null) {
                        emit(emptyList())
                    } else {
                        emitAll(remotePhotosDataSource.allItemsFlow(baseUrl))
                    }
                }.onStart { emit(emptyList()) }
            }
        }
    }

    override suspend fun syncMedia(
        media: MediaItemDto,
        mediaUri: String
    ): NetworkResult<Unit> {
        val baseUrl = endpointResolver.resolve()
        if (baseUrl == null) {
            val errorMessage = "syncMedia failed because the server could not be located"
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

    override fun observeConnectionState(): Flow<ServerConnectionState> = endpointResolver.state

    /**
     * Derived view of the connection for callers that only gate on whether traffic can flow.
     * Kept as a plain Boolean so the sync gates keep their existing semantics.
     */
    private val isSeverConnected: Flow<Boolean> =
        endpointResolver.state.map { it.isConnected }.distinctUntilChanged()

    override fun observeServerConnection(): Flow<Boolean> = isSeverConnected
}
