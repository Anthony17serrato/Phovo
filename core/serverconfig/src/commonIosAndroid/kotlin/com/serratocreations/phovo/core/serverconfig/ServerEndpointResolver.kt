package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Finds the address of the server this client is paired with.
 *
 * The client persists *which* server it belongs to, not *where* it is. This resolves the second
 * from the first: it tries the cached address, verifies the identity that answers, and re-browses
 * mDNS when that fails. That is what makes a DHCP change, a move between Wi-Fi and Ethernet, or a
 * router reboot recoverable without the user re-pairing.
 *
 * Implementations must be safe to call from many callers at once — the sync workers all fail
 * together — and must not turn a burst of failures into a burst of mDNS browses.
 */
interface ServerEndpointResolver {
    /** Current view of the connection, including which address is in use when connected. */
    val state: StateFlow<ServerConnectionState>

    /**
     * @param force skip the cached address and re-browse, subject to rate limiting.
     * @return the base URL to use, or null if the server could not be located.
     */
    suspend fun resolve(force: Boolean = false): BaseUrl?

    /** Signals that the address previously handed out stopped working. */
    fun invalidate()
}
