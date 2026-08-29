package com.serratocreations.phovo.data.server.data.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Reads this machine's own network addresses from the host OS.
 *
 * Exists so the question "which of our addresses can a client on the LAN actually reach us at" has
 * one answer in one place. Getting it wrong is silent: the address is published over mDNS and
 * stored by every client, so an unroutable choice presents as a server that is simply offline.
 */
class HostAddressDataSource(
    logger: PhovoLogger
) {
    private val log = logger.withTag("HostAddressDataSource")

    private companion object {
        /** Any off-link address will do; it is used for a route lookup and never contacted. */
        const val ROUTE_LOOKUP_ADDRESS = "1.1.1.1"
        const val ROUTE_LOOKUP_PORT = 53
        // TODO Loopback is a placeholder, not an answer — no client can reach another machine's
        //  loopback, so returning it turns "this host has no network" into an address that is
        //  advertised and stored but can never work. Make hostIPv4() nullable and surface a
        //  "no network connection" state instead, alongside the server-failed-to-start error.
        const val LOOPBACK_ADDRESS = "127.0.0.1"

        /**
         * Names hypervisors and container runtimes use for host-only bridges. Used to order rather
         * than to exclude: macOS Internet Sharing runs a real LAN over `bridge100`, so one of these
         * can legitimately be the only interface a client could reach.
         *
         * Tunnels are absent because the capability filters already drop them.
         */
        val VIRTUAL_BRIDGE_PREFIXES = listOf(
            "bridge",   // Parallels Desktop, and macOS Internet Sharing
            "vmnet",    // VMware
            "vboxnet",  // VirtualBox
            "docker",
            "br-",      // Docker user defined bridges
            "virbr"     // libvirt
        )
    }

    /**
     * The IPv4 address clients on the LAN can actually reach this machine at.
     *
     * Interface enumeration order is not meaningful, and hypervisor bridges (Parallels, VMware,
     * Docker) hand out private addresses indistinguishable from a real LAN address while being
     * reachable only from this host. [NetworkInterface.isVirtual] does not identify them — it only
     * reports subinterfaces such as `en0:1`.
     *
     * First choice is the address the OS itself routes outbound traffic through, since that
     * interface is by definition one this machine shares with others. That fails whenever a VPN
     * holds the default route, because a tunnel cannot carry mDNS and so is not a candidate. The
     * fallback then relies on [candidateAddresses] being ordered, which is where the name heuristic
     * earns its place.
     */
    fun hostIPv4(): String {
        val candidates = candidateAddresses()
        val defaultRouteAddress = defaultRouteIPv4()

        log.i {
            "Host IPv4 candidates: ${candidates.map { it.hostAddress }}, " +
                "default route: ${defaultRouteAddress?.hostAddress}"
        }

        // The routed address is authoritative when it also survived the interface scan.
        if (defaultRouteAddress != null && defaultRouteAddress in candidates) {
            return defaultRouteAddress.hostAddress
        }

        // Otherwise the route leaves through an interface the scan rejected — a VPN tunnel, say —
        // so fall back to a private address a client on the same LAN could route to.
        return candidates.firstOrNull { it.isSiteLocalAddress }?.hostAddress
            ?: defaultRouteAddress?.hostAddress
            ?: candidates.firstOrNull()?.hostAddress
            ?: LOOPBACK_ADDRESS
    }

    /**
     * Every IPv4 address on an interface that could plausibly carry LAN traffic.
     *
     * Membership is decided by interface capability, since a point-to-point or non-multicast
     * interface cannot carry mDNS. Order is decided by name, with hypervisor bridges last, because
     * enumeration order is arbitrary and would otherwise hand a Parallels or Docker bridge to
     * callers ahead of the real LAN. Ordering rather than excluding keeps such a bridge usable when
     * it is the only interface there is.
     */
    fun candidateAddresses(): List<Inet4Address> = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint && it.supportsMulticast() }
            .sortedBy { it.isProbablyVirtualBridge() }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { it.isUsableHostAddress() }
            .toList()
    } catch (e: Exception) {
        log.e(e) { "Unable to enumerate network interfaces" }
        emptyList()
    }

    /**
     * The local address the OS would use to reach the outside world — that is, the address on
     * whichever interface holds the default route.
     *
     * The JDK cannot read the routing table, so this asks the kernel indirectly. Connecting a
     * *UDP* socket performs a route lookup and binds the socket to the chosen local interface,
     * but unlike TCP it sends no packets and needs no reachable peer, so this works offline.
     * [ROUTE_LOOKUP_ADDRESS] is never contacted; it only has to be off-link so the lookup
     * resolves via the default route rather than a directly attached one.
     *
     * @return null when there is no default route at all, leaving [candidateAddresses] to decide.
     */
    private fun defaultRouteIPv4(): Inet4Address? = try {
        DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName(ROUTE_LOOKUP_ADDRESS), ROUTE_LOOKUP_PORT)
            (socket.localAddress as? Inet4Address)?.takeIf { it.isUsableHostAddress() }
        }
    } catch (e: Exception) {
        log.e(e) { "Unable to resolve default route address" }
        null
    }

    private fun NetworkInterface.isProbablyVirtualBridge(): Boolean {
        val lowercaseName = name.lowercase()
        return VIRTUAL_BRIDGE_PREFIXES.any { lowercaseName.startsWith(it) }
    }

    /** Excludes the wildcard, loopback and self-assigned (169.254.0.0/16) addresses. */
    private fun Inet4Address.isUsableHostAddress(): Boolean =
        !isAnyLocalAddress && !isLoopbackAddress && !isLinkLocalAddress
}
