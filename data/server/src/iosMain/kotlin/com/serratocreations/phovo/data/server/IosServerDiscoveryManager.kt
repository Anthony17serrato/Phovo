package com.serratocreations.phovo.data.server

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ServerTxtRecord
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.data.model.DiscoveredServer
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
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
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
                        val server = sender.toDiscoveredServer()
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
                didFindService.scheduleInRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
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
        browser.scheduleInRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
        browser.searchForServicesOfType("_phovo._tcp", inDomain = "local.")

        awaitClose {
            browser.removeFromRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
            browser.stop()
            servicesToResolve.forEach {
                it.removeFromRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
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
        log.i { "Connecting to discovered server: ${server.url} id: ${server.serverId}" }
        serverConfigRepository.updateClientServerConfig(
            serverUrl = server.url,
            serverId = server.serverId,
            serviceName = server.name
        )
    }

    private fun NSNetService.toDiscoveredServer(): DiscoveredServer {
        // Prefer IPv4. The list is ordered by the resolver, not by usefulness, and an IPv6 literal
        // needs bracketing before it can go in a URL — previously the first entry was taken as-is.
        val resolvedAddresses = addresses
            ?.mapNotNull { (it as? NSData)?.let(::ipAddressFromData) }
            .orEmpty()
            .map { it.removeSuffix(".") }
        val host = resolvedAddresses.firstOrNull { !it.contains(':') }
            ?: resolvedAddresses.firstOrNull()
            ?: hostName?.removeSuffix(".")
            ?: "localhost"

        val advertisement = TXTRecordData()
            ?.let { NSNetService.dictionaryFromTXTRecordData(it) }
            ?.let { txtDictionary ->
                val properties = txtDictionary.entries.associate { entry ->
                    val key = entry.key as? String ?: ""
                    val value = (entry.value as? NSData)?.let { data ->
                        NSString.create(data, NSUTF8StringEncoding) as String?
                    }
                    key to value
                }
                ServerTxtRecord.decode(properties)
            }

        return DiscoveredServer(
            name = name,
            ipAddress = host,
            port = port.toInt(),
            serverId = advertisement?.serverId,
            scheme = advertisement?.scheme ?: ServerTxtRecord.SCHEME_HTTP,
            alternateAddresses = (advertisement?.addresses.orEmpty() + resolvedAddresses)
                .distinct()
                .filterNot { it == host }
        )
    }
}