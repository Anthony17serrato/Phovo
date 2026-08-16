package com.serratocreations.phovo.data.server

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.NetworkFailure
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerEndpointResolver
import com.serratocreations.phovo.data.server.data.model.DiscoveredServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ServerEndpointResolverImpl(
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val serverDiscoveryManager: ServerDiscoveryManager,
    private val healthProbe: ServerHealthProbe,
    private val applicationScope: CoroutineScope,
    logger: PhovoLogger
) : ServerEndpointResolver {

    private companion object {
        /** How long to browse mDNS before giving up on finding the server. */
        val DISCOVERY_TIMEOUT = 10.seconds

        /**
         * Floor on the gap between forced re-browses. Every sync worker calls invalidate() when its
         * upload fails, so without this a single outage becomes an mDNS storm.
         */
        val MIN_REBROWSE_INTERVAL = 30.seconds
    }

    private val log = logger.withTag("ServerEndpointResolver")

    private val _state = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Unknown)
    override val state = _state.asStateFlow()

    private val resolveMutex = Mutex()
    /** In-flight resolution, shared by every concurrent caller so one browse serves all of them. */
    private var inFlight: Deferred<BaseUrl?>? = null
    private var lastForcedBrowse: TimeSource.Monotonic.ValueTimeMark? = null
    private var cachedEndpoint: BaseUrl? = null

    override fun invalidate() {
        cachedEndpoint = null
    }

    override suspend fun resolve(force: Boolean): BaseUrl? {
        if (!force) cachedEndpoint?.let { return it }

        val resolution = resolveMutex.withLock {
            inFlight?.takeIf { it.isActive } ?: applicationScope.async {
                try {
                    performResolve(force)
                } finally {
                    resolveMutex.withLock { inFlight = null }
                }
            }.also { inFlight = it }
        }
        return resolution.await()
    }

    private suspend fun performResolve(force: Boolean): BaseUrl? {
        val config = serverConfigRepository.observeServerConfig().first()
        if (config == null) {
            _state.value = ServerConnectionState.Unknown
            return null
        }
        val expectedServerId = config.serverId

        // The cached address is right almost always, so try it before spending 10s on a browse.
        if (!force) {
            val cachedUrl = serverConfigRepository.cachedServerUrl()
            if (cachedUrl != null) {
                // Stay Connected while re-verifying an address that is already known good. The
                // health poll runs through here every 15s, and announcing Checking each time made
                // the UI flash "server offline" between every successful probe.
                if (_state.value !is ServerConnectionState.Connected) {
                    _state.value = ServerConnectionState.Checking
                }
                when (val outcome = verify(cachedUrl, expectedServerId)) {
                    is Verified.Ok -> return accept(cachedUrl)
                    is Verified.WrongServer -> {
                        // Do not fall through to the cache again; go looking for the real one.
                        log.i { "Identity mismatch at $cachedUrl, re-resolving" }
                    }
                    is Verified.Unreachable -> log.i { "Cached address unreachable: ${outcome.reason}" }
                }
            }
        }

        // Without an identity there is nothing to search for — an unverifiable legacy pairing can
        // only ever use the address it was given.
        if (expectedServerId == null) {
            _state.value = ServerConnectionState.Unreachable(NetworkFailure.Unreachable)
            return null
        }

        // Throttle every browse, not just forced ones: the periodic health poll also lands here
        // once the cached address stops answering, and mDNS browsing is not cheap.
        if (!mayRebrowse()) {
            log.i { "Skipping re-browse, throttled" }
            _state.value = ServerConnectionState.Unreachable(NetworkFailure.Unreachable)
            return null
        }
        lastForcedBrowse = TimeSource.Monotonic.markNow()

        _state.value = ServerConnectionState.Resolving
        val match = browseForServer(expectedServerId)
        if (match == null) {
            _state.value = ServerConnectionState.Unreachable(NetworkFailure.Unreachable)
            return null
        }

        // A multi-homed server advertises several addresses; only some may be routable from here.
        for (candidate in match.candidateUrls) {
            val candidateUrl = BaseUrl(candidate)
            if (verify(candidateUrl, expectedServerId) is Verified.Ok) {
                log.i { "Server $expectedServerId resolved to $candidate" }
                serverConfigRepository.updateCachedServerUrl(candidate)
                return accept(candidateUrl)
            }
        }

        _state.value = ServerConnectionState.Unreachable(NetworkFailure.Unreachable)
        return null
    }

    private fun accept(baseUrl: BaseUrl): BaseUrl {
        cachedEndpoint = baseUrl
        _state.value = ServerConnectionState.Connected(baseUrl)
        return baseUrl
    }

    private fun mayRebrowse(): Boolean {
        val last = lastForcedBrowse ?: return true
        return last.elapsedNow() >= MIN_REBROWSE_INTERVAL
    }

    private suspend fun browseForServer(expectedServerId: String): DiscoveredServer? =
        withTimeoutOrNull(DISCOVERY_TIMEOUT) {
            serverDiscoveryManager.discoverServers()
                .first { servers -> servers.any { it.serverId == expectedServerId } }
                .first { it.serverId == expectedServerId }
        }

    private suspend fun verify(baseUrl: BaseUrl, expectedServerId: String?): Verified =
        when (val result = healthProbe.probe(baseUrl)) {
            is ProbeResult.Healthy -> {
                val actual = result.health.serverId
                when {
                    expectedServerId == null -> {
                        // Pairing was made without an identity — manual entry, or against a server
                        // that had no health endpoint. Adopt what it reports now, otherwise this
                        // client could never find the server again once its address changed.
                        log.i { "Adopting server identity $actual for an unidentified pairing" }
                        serverConfigRepository.adoptServerId(actual)
                        Verified.Ok
                    }
                    expectedServerId == actual -> Verified.Ok
                    else -> {
                        _state.value = ServerConnectionState.IdentityMismatch(
                            expectedServerId = expectedServerId,
                            actualServerId = actual
                        )
                        Verified.WrongServer
                    }
                }
            }
            is ProbeResult.Failed -> Verified.Unreachable(result.reason)
        }

    private sealed interface Verified {
        data object Ok : Verified
        data object WrongServer : Verified
        data class Unreachable(val reason: NetworkFailure) : Verified
    }
}
