package com.serratocreations.phovo.core.model.network

/**
 * The client's view of its connection to the Phovo server.
 *
 * Replaces a bare `Boolean`, which could not tell "no server configured yet" apart from "configured
 * but unreachable" — both surfaced identically, so a client that had never been paired reported a
 * problem it did not have.
 *
 * Callers reduce this to whatever they need. [isConnected] is provided for the common case of
 * gating work on connectivity, and using it rather than matching exhaustively means later additions
 * to this hierarchy are not breaking changes.
 */
sealed interface ServerConnectionState {
    /** No server is configured, or the first reachability check has not completed yet. */
    data object Unknown : ServerConnectionState

    /** A reachability check is currently in flight. */
    data object Checking : ServerConnectionState

    /** The server answered and is ready to serve requests. */
    data object Connected : ServerConnectionState

    /** The server is configured but could not be reached. */
    data class Unreachable(val reason: NetworkFailure) : ServerConnectionState

    /**
     * Something answered at the stored address, but it is not the server this client paired with.
     * Typically, a DHCP lease handing that address to a different machine.
     *
     * Unlike [Unreachable] this will not heal by waiting, and it has to block uploads: checking
     * identity exists so photos are never sent to a stranger's machine.
     */
    data object IdentityMismatch : ServerConnectionState
}

/** True when requests to the server can currently succeed. */
val ServerConnectionState.isConnected: Boolean
    get() = this is ServerConnectionState.Connected
