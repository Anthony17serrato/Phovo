package com.serratocreations.phovo.data.server

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Finds the paired server again after its address changes.
 *
 * The client stores which server it belongs to, and that identity outlives any address: a DHCP
 * lease expires, a laptop moves from Wi-Fi to an Ethernet dock, a router reboots. mDNS is the only
 * way to learn the new address on a network with no other infrastructure, and the identity in the
 * TXT record is what tells a browse result apart from any other Phovo server on the network.
 *
 * Lives in this module because it needs both halves: discovery is here, and the connection state
 * comes from data/photos, which this module already depends on.
 */
class ServerAddressResolver(
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val remoteMediaRepository: RemoteMediaRepository,
    private val serverDiscoveryManager: ServerDiscoveryManager,
    private val applicationScope: CoroutineScope,
    logger: PhovoLogger
) {
    private val log = logger.withTag(TAG)

    private companion object {
        const val TAG = "ServerAddressResolver"

        /** How long to browse before giving up on this attempt. */
        val DISCOVERY_TIMEOUT = 10.seconds

        /** Gap between attempts while the server stays missing. */
        val RETRY_INTERVAL = 30.seconds
    }

    fun start() {
        applicationScope.launch {
            remoteMediaRepository.observeConnectionState().collectLatest { connectionState ->
                if (connectionState.needsAddressLookup().not()) return@collectLatest
                // The connection state is distinct until changed, so a failure that persists gives
                // no further emissions to retry from. collectLatest cancels this loop the moment
                // the connection recovers or the pairing changes.
                while (currentCoroutineContext().isActive) {
                    lookUpAddress()
                    delay(RETRY_INTERVAL)
                }
            }
        }
    }

    private fun ServerConnectionState.needsAddressLookup(): Boolean = when (this) {
        // Unreachable covers the server having moved. A mismatch means the stored address now
        // belongs to a different Phovo server, so the paired one is elsewhere or gone.
        is ServerConnectionState.Unreachable,
        ServerConnectionState.IdentityMismatch -> true
        ServerConnectionState.Connected,
        ServerConnectionState.Checking,
        ServerConnectionState.Unknown -> false
    }

    private suspend fun lookUpAddress() {
        val serverId = serverConfigRepository.observeServerConfig().first()?.serverId ?: return

        log.i { "Looking for server $serverId on the network" }
        val match = withTimeoutOrNull(DISCOVERY_TIMEOUT) {
            serverDiscoveryManager.discoverServers()
                .mapNotNull { servers -> servers.firstOrNull { it.serverId == serverId } }
                .first()
        }

        if (match == null) {
            log.i { "Server $serverId did not answer discovery" }
            return
        }

        // Writing the address is the whole handoff: the health poll observes the config, so it
        // restarts against the new address on its own.
        log.i { "Server $serverId found at ${match.url}" }
        serverConfigRepository.updateCachedServerUrl(match.url)
    }
}
