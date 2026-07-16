package com.serratocreations.phovo.data.server

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import platform.Foundation.NSData
import platform.Foundation.NSLock
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.darwin.NSObject
import platform.posix.getnameinfo
import platform.posix.sockaddr
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class IosServerDiscoveryManager(
    private val serverConfigRepository: ServerConfigRepository,
    mainApplicationScope: CoroutineScope,
    logger: PhovoLogger
) : ServerDiscoveryManager {
    private val log = logger.withTag("IosServerDiscoveryManager")
    private val lock = NSLock()
    private val activeDelegates = mutableSetOf<Any>()

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

    private val serverDiscoverySharedFlow = callbackFlow {
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
                        val ip = addresses?.firstNotNullOfOrNull { data ->
                            val nsData = data as? NSData
                            nsData?.let { ipAddressFromData(it) }
                        } ?: sender.hostName ?: "localhost"

                        val cleanedHost =
                            if (ip.endsWith(".")) ip.substring(0, ip.length - 1) else ip

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

                lock.lock()
                try {
                    activeDelegates.add(serviceDelegate)
                } finally {
                    lock.unlock()
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

        lock.lock()
        try {
            activeDelegates.add(browserDelegate)
        } finally {
            lock.unlock()
        }
        browser.setDelegate(browserDelegate)
        browser.searchForServicesOfType("_phovo._tcp", inDomain = "local.")

        awaitClose {
            browser.stop()
            servicesToResolve.forEach {
                it.setDelegate(null)
                it.stop()
            }
            lock.lock()
            try {
                activeDelegates.clear()
            } finally {
                lock.unlock()
            }
        }
    }.shareIn(
        scope = mainApplicationScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        replay = 1
    )

    override fun discoverServers(): Flow<List<DiscoveredServer>> = serverDiscoverySharedFlow

    override suspend fun connectToServer(server: DiscoveredServer) {
        log.i { "Connecting to discovered server: ${server.url}" }
        serverConfigRepository.updateClientServerConfig(server.url)
    }
}