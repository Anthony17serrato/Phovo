package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.model.network.isConnected
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val log = logger.withTag(TAG)

    companion object {
        private const val TAG = "RemoteMediaRepository"
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
                connectionState.drop(1).first { it.isConnected }
            }
        )
    }

    // TODO: Most likely there is a more sophisticated networking method to check alive
    //  then pinging every X seconds(Investigate)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val connectionState: Flow<ServerConnectionState> =
        serverConfigRepository.observeServerConfig()
            // Keyed on the address alone. The probe writes the server's name back into the config,
            // and restarting on every config change would let the poll cancel itself.
            .map { it?.serverBaseUrlString }
            .distinctUntilChanged()
            .flatMapLatest { baseUrl ->
                if (baseUrl == null) {
                    // Not configured is distinct from unreachable: a client that has never been
                    // paired does not have a problem to report.
                    flowOf<ServerConnectionState>(ServerConnectionState.Unknown)
                } else {
                    flow<ServerConnectionState> {
                        emit(ServerConnectionState.Checking)
                        while (currentCoroutineContext().isActive) {
                            yield()
                            emit(checkConnection(baseUrl))
                            delay(CHECK_ALIVE_DELAY)
                        }
                    }
                }
            }
            // The poll re-emits the same state every CHECK_ALIVE_DELAY; only changes are interesting.
            .distinctUntilChanged()
            .shareIn(
                scope = applicationScope,
                started = SharingStarted.Lazily,
                replay = 1
            )

    private suspend fun checkConnection(baseUrl: BaseUrl): ServerConnectionState =
        when (val result = remotePhotosDataSource.fetchServerHealth(baseUrl)) {
            is NetworkResult.NetworkSuccess -> {
                cacheServerName(result.data.serverName)
                ServerConnectionState.Connected
            }
            is NetworkResult.NetworkError -> {
                // The classified reason is a diagnostic, not something to show: to the user this is
                // just "the server is offline" whether it timed out, refused, or failed to resolve.
                log.i { "Server unreachable at ${baseUrl.value}: ${result.failure} (${result.message})" }
                ServerConnectionState.Unreachable(result.failure)
            }
        }

    /**
     * Keeps the stored name in step with what the server calls itself. The name the client paired
     * with came from mDNS, which renames services on collision, and the user can rename the server
     * at any time — so the health response is the only authority, and the poll is what keeps it
     * fresh. Written only on a change, since an unconditional write would wake every observer of
     * the config every [CHECK_ALIVE_DELAY].
     */
    private suspend fun cacheServerName(serverName: String) {
        val cachedName = serverConfigRepository.observeServerConfig().first()?.serverName
        if (cachedName != serverName) {
            log.i { "Caching server name: $cachedName -> $serverName" }
            serverConfigRepository.updateCachedServerName(serverName)
        }
    }

    override fun observeConnectionState(): Flow<ServerConnectionState> = connectionState
}