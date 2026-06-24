@file:OptIn(ExperimentalForeignApi::class)

package com.serratocreations.phovo.core.serverconfig.discovery

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.*

class IosServerDiscoveryManager(
    private val serverConfigRepository: ServerConfigRepository,
    logger: PhovoLogger
) : ServerDiscoveryManager {
    private val log = logger.withTag("IosServerDiscoveryManager")
    private val lock = NSLock()

    private fun ipAddressFromData(data: NSData): String? {
        val bytes = data.bytes ?: return null
        val socketAddress = bytes.reinterpret<sockaddr>()
        memScoped {
            val host = allocArray<ByteVar>(1025) // NI_MAXHOST
            val result = getnameinfo(
                socketAddress,
                data.length.toUInt(),
                host,
                1025.toUInt(),
                null,
                0.toUInt(),
                2 // NI_NUMERICHOST
            )
            if (result == 0) {
                return host.toKString()
            }
        }
        return null
    }

    override fun startDiscovery(): Flow<List<DiscoveredServer>> = callbackFlow {
        val discoveredServers = mutableMapOf<String, DiscoveredServer>()
        val servicesToResolve = mutableListOf<NSNetService>()

        val browser = NSNetServiceBrowser()

        val browserDelegate = object : NSObject(), NSNetServiceBrowserDelegateProtocol {
            @ObjCSignatureOverride
            override fun netServiceBrowser(
                browser: NSNetServiceBrowser,
                didFindService: NSNetService,
                moreComing: Boolean
            ) {
                log.i { "didFindService: ${didFindService.name}" }
                servicesToResolve.add(didFindService)

                val serviceDelegate = object : NSObject(), NSNetServiceDelegateProtocol {
                    override fun netServiceDidResolveAddress(sender: NSNetService) {
                        log.i { "netServiceDidResolveAddress: ${sender.name}" }
                        val addresses = sender.addresses
                        val ip = addresses?.mapNotNull { data ->
                            val nsData = data as? NSData
                            nsData?.let { ipAddressFromData(it) }
                        }?.firstOrNull() ?: sender.hostName ?: "localhost"

                        val cleanedHost = if (ip.endsWith(".")) ip.substring(0, ip.length - 1) else ip

                        val server = DiscoveredServer(
                            name = sender.name,
                            ipAddress = cleanedHost,
                            port = sender.port.toInt()
                        )
                        lock.lock()
                        try {
                            discoveredServers[sender.name] = server
                            trySend(discoveredServers.values.toList())
                        } finally {
                            lock.unlock()
                        }
                    }

                    override fun netService(sender: NSNetService, didNotResolve: Map<Any?, *>) {
                        log.e { "didNotResolve: ${sender.name}" }
                    }
                }

                didFindService.setDelegate(serviceDelegate)
                didFindService.resolveWithTimeout(5.0)
            }

            @ObjCSignatureOverride
            override fun netServiceBrowser(
                browser: NSNetServiceBrowser,
                didRemoveService: NSNetService,
                moreComing: Boolean
            ) {
                log.i { "didRemoveService: ${didRemoveService.name}" }
                lock.lock()
                try {
                    discoveredServers.remove(didRemoveService.name)
                    trySend(discoveredServers.values.toList())
                } finally {
                    lock.unlock()
                }
            }
        }

        browser.setDelegate(browserDelegate)
        browser.searchForServicesOfType("_phovo._tcp", inDomain = "local.")

        awaitClose {
            browser.stop()
            servicesToResolve.forEach { it.stop() }
        }
    }

    override fun stopDiscovery() {
        // Handled via flow cancellation
    }

    override suspend fun connectToServer(server: DiscoveredServer) {
        log.i { "Connecting to discovered server: ${server.url}" }
        serverConfigRepository.updateClientServerConfig(server.url)
    }
}
