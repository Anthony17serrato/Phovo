package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.NetworkFailure
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.server.data.model.DiscoveredServer

sealed interface PairingResult {
    data object Paired : PairingResult

    /** Something answered, but it is not a configured Phovo server. */
    data object NotAPhovoServer : PairingResult

    /** Nothing answered at that address. */
    data object Unreachable : PairingResult
}

/**
 * Pairs this client with a server, by either route.
 *
 * Both routes have to end up storing the same three things, so they belong together. Discovery
 * learns the identity from the TXT record; a typed address learns it by asking the server. Pairing
 * without an identity is what left a client unable to find its server again after the address
 * changed, so neither route is allowed to skip it.
 */
class PairServerUseCase(
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val remotePhotosDataSource: MediaNetworkDataSource,
    logger: PhovoLogger
) {
    private val log = logger.withTag(TAG)

    private companion object {
        const val TAG = "PairServerUseCase"
        const val DEFAULT_SCHEME = "http://"
    }

    /**
     * Pairs with a server discovery already vouched for. Nothing is probed: the TXT record carried
     * the identity, which is the only thing a probe would add, so this cannot fail the way a typed
     * address can and has nothing to report.
     */
    suspend operator fun invoke(server: DiscoveredServer) {
        log.i { "Pairing with discovered server ${server.url} id: ${server.serverId}" }
        serverConfigRepository.updateClientServerConfig(
            serverUrl = server.url,
            serverId = server.serverId,
            // Only a placeholder label: the first health probe replaces it with whatever the server
            // calls itself, which mDNS may have renamed to avoid a collision on the network.
            serverName = server.name
        )
    }

    /**
     * Pairs with a typed address, asking the server who it is before accepting it. Discovery gets
     * an identity for free from the TXT record; this is the equivalent handshake for an address
     * nothing has vouched for.
     */
    suspend operator fun invoke(address: String): PairingResult {
        val baseUrl = normalize(address)
        log.i { "Probing ${baseUrl.value} before pairing" }

        return when (val result = remotePhotosDataSource.fetchServerHealth(baseUrl)) {
            is NetworkResult.NetworkSuccess -> {
                val health = result.data
                log.i { "Pairing with ${baseUrl.value} id: ${health.serverId}" }
                serverConfigRepository.updateClientServerConfig(
                    serverUrl = baseUrl.value,
                    serverId = health.serverId,
                    serverName = health.serverName
                )
                PairingResult.Paired
            }
            is NetworkResult.NetworkError -> {
                log.i { "Not pairing with ${baseUrl.value}: ${result.failure} (${result.message})" }
                when (result.failure) {
                    // Answered, but with a status or a body a Phovo server would not produce.
                    NetworkFailure.HttpStatus,
                    NetworkFailure.Malformed -> PairingResult.NotAPhovoServer
                    NetworkFailure.Unreachable,
                    NetworkFailure.Timeout,
                    NetworkFailure.Unknown -> PairingResult.Unreachable
                }
            }
        }
    }

    /**
     * People type `192.168.1.10:8080` rather than a full URL, and a missing scheme would fail as
     * "nothing answered" instead of as the typo it is.
     */
    private fun normalize(address: String): BaseUrl {
        val trimmed = address.trim().trimEnd('/')
        return BaseUrl(if (trimmed.contains("://")) trimmed else "$DEFAULT_SCHEME$trimmed")
    }
}
