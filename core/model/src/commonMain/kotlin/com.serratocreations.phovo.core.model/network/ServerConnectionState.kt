package com.serratocreations.phovo.core.model.network

/**
 * The client's view of its connection to the Phovo server.
 *
 * This replaces a bare `Boolean`, which could not distinguish "no server configured yet" from
 * "configured but the server is down" — both surfaced as a red status dot. Consumers that only
 * need to know whether traffic can flow should use [isConnected] rather than matching exhaustively,
 * so that later additions to this hierarchy do not become breaking changes.
 */
sealed interface ServerConnectionState {
    /** No server is configured, or the first reachability check has not completed yet. */
    data object Unknown : ServerConnectionState

    /** A reachability check is currently in flight. */
    data object Checking : ServerConnectionState

    /** The cached address failed and the server is being looked up over mDNS. */
    data object Resolving : ServerConnectionState

    /** The server answered and is ready to serve requests at [baseUrl]. */
    data class Connected(val baseUrl: BaseUrl) : ServerConnectionState

    /** The server is configured but could not be reached. */
    data class Unreachable(val reason: NetworkFailure) : ServerConnectionState

    /**
     * Something answered at the expected address, but it is not the server this client paired
     * with — typically a DHCP reshuffle handing the old address to a different machine.
     *
     * Unlike [Unreachable] this is not transient and will not heal by retrying, so callers must
     * stop polling the address rather than hammering a stranger's machine. It must also block
     * uploads: the whole point of verifying identity is not to send someone's photos elsewhere.
     */
    data class IdentityMismatch(
        val expectedServerId: String,
        val actualServerId: String?
    ) : ServerConnectionState

    val isConnected: Boolean get() = this is Connected
}
