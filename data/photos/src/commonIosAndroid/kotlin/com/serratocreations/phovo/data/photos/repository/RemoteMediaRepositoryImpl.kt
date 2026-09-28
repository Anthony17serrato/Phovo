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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private val heartBeatEventFlow = MutableSharedFlow<Unit>()

    init {
        applicationScope.launch {
            while (this.isActive) {
                yield()
                heartBeatEventFlow.emit(Unit)
                delay(CHECK_ALIVE_DELAY)
            }
        }
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
        mediaUri: String,
        onBytesSent: (bytesSent: Long) -> Unit
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
                observeConnectionState().first { it.isConnected }
            },
            onBytesSent = onBytesSent
        )
    }

    // TODO: Most likely there is a more sophisticated networking method to check alive
    //  then pinging every X seconds(Investigate)
    /**
     * Do not use directly, use [observeConnectionState] which includes on start logic
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val connectionState: SharedFlow<ServerConnectionState> =
        combine(
            heartBeatEventFlow.onStart { emit(Unit) },
            serverConfigRepository.observeServerConfig()
        ) { _, config ->
            if (config == null) {
                // Not configured is distinct from unreachable: a client that has never been
                // paired does not have a problem to report.
                ServerConnectionState.NotConfigured
            } else {
                checkConnection(config.serverBaseUrlString, config.serverId)
            }
        }
            .shareIn(
                scope = applicationScope,
                started = SharingStarted.Lazily,
                replay = 0
            )

    private suspend fun checkConnection(
        baseUrl: BaseUrl,
        expectedServerId: String
    ): ServerConnectionState =
        when (val result = remotePhotosDataSource.fetchServerHealth(baseUrl)) {
            is NetworkResult.NetworkSuccess -> {
                val health = result.data
                if (expectedServerId != health.serverId) {
                    log.e {
                        "Identity mismatch at ${baseUrl.value}: paired with $expectedServerId " +
                            "but ${health.serverId} answered"
                    }
                    // The name is deliberately not cached here; it belongs to someone else's server.
                    ServerConnectionState.IdentityMismatch
                } else {
                    serverConfigRepository.updateCachedServerNameIfNew(health.serverName)
                    ServerConnectionState.Connected
                }
            }
            is NetworkResult.NetworkError -> {
                // The classified reason is a diagnostic, not something to show: to the user this is
                // just "the server is offline" whether it timed out, refused, or failed to resolve.
                log.i { "Server unreachable at ${baseUrl.value}: ${result.failure} (${result.message})" }
                ServerConnectionState.Unreachable(result.failure)
            }
        }


    override fun observeConnectionState(): Flow<ServerConnectionState> =
        connectionState.onStart {
            heartBeatEventFlow.emit(Unit)
            emit(ServerConnectionState.Checking)
        }.distinctUntilChanged()
}