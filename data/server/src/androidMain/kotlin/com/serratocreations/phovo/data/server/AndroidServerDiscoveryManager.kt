package com.serratocreations.phovo.data.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ServerTxtRecord
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.data.model.DiscoveredServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

class AndroidServerDiscoveryManager(
    private val context: Context,
    private val serverConfigRepository: ServerConfigRepository,
    applicationScope: CoroutineScope,
    logger: PhovoLogger
) : ServerDiscoveryManager {
    private val log = logger.withTag("AndroidServerDiscoveryManager")
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val serverDiscoverySharedFlow = callbackFlow {
        val discoveredServers = mutableMapOf<String, DiscoveredServer>()
        val discoveredServersMutex = Mutex()
        val activeCallbacks = mutableMapOf<String, Any>()

        @Suppress("DEPRECATION") // Only used on older SDKs
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                log.e { "Resolve failed: $errorCode for ${serviceInfo.serviceName}" }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val hostAddress = serviceInfo.host?.hostAddress ?: return
                val sanitizedHost = hostAddress.sanitizeHost()
                log.i { "Service resolved: ${serviceInfo.serviceName} at $sanitizedHost:${serviceInfo.port}" }

                val server = serviceInfo.toDiscoveredServer(sanitizedHost)

                launch {
                    discoveredServersMutex.withLock {
                        discoveredServers[serviceInfo.serviceName] = server
                        send(discoveredServers.values.toList())
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun NsdServiceInfo.serviceInfoCallback() = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                log.e { "Callback registration failed: $errorCode for ${this@serviceInfoCallback.serviceName}" }
            }

            override fun onServiceUpdated(resolvedInfo: NsdServiceInfo) {
                // Prefer IPv4: a bare IPv6 literal needs bracketing in a URL and is more likely to
                // be unroutable on a home LAN.
                val hostAddress = resolvedInfo.hostAddresses
                    .sortedBy { it is java.net.Inet6Address }
                    .firstOrNull()?.hostAddress ?: return
                val sanitizedHost = hostAddress.sanitizeHost()
                log.i { "Service updated: ${resolvedInfo.serviceName} at $sanitizedHost:${resolvedInfo.port}" }

                val server = resolvedInfo.toDiscoveredServer(sanitizedHost)
                launch {
                    discoveredServersMutex.withLock {
                        discoveredServers[resolvedInfo.serviceName] = server
                        send(discoveredServers.values.toList())
                    }
                }
            }

            override fun onServiceLost() {
                log.i { "Service Lost ${this@serviceInfoCallback.serviceName}" }
                launch {
                    discoveredServersMutex.withLock {
                        discoveredServers.remove(this@serviceInfoCallback.serviceName)
                        send(discoveredServers.values.toList())
                    }
                }
            }

            override fun onServiceInfoCallbackUnregistered() {
                log.i { "Callback unregistered for ${this@serviceInfoCallback.serviceName}" }
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                log.e { "Discovery start failed: $errorCode" }
                close(Exception("Discovery start failed: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                log.e { "Discovery stop failed: $errorCode" }
            }

            override fun onDiscoveryStarted(serviceType: String) {
                log.i { "Discovery started: $serviceType" }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                log.i { "Discovery stopped: $serviceType" }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                log.i { "Service found: ${serviceInfo.serviceName}" }
                if (serviceInfo.serviceType.contains("_phovo._tcp")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        if (activeCallbacks.containsKey(serviceInfo.serviceName).not()) {
                            val serviceInfoCallback = serviceInfo.serviceInfoCallback()
                            activeCallbacks[serviceInfo.serviceName] = serviceInfoCallback
                            nsdManager.registerServiceInfoCallback(
                                serviceInfo,
                                context.mainExecutor,
                                serviceInfoCallback
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(serviceInfo, resolveListener)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                log.i { "Service lost: ${serviceInfo.serviceName}" }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val callback =
                        activeCallbacks.remove(serviceInfo.serviceName) as? NsdManager.ServiceInfoCallback
                    if (callback != null) {
                        nsdManager.unregisterServiceInfoCallback(callback)
                    }
                }
                launch {
                    discoveredServersMutex.withLock {
                        discoveredServers.remove(serviceInfo.serviceName)
                        send(discoveredServers.values.toList())
                    }
                }
            }
        }

        nsdManager.discoverServices("_phovo._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                log.e(e) { "Error stopping discovery" }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activeCallbacks.values.forEach { callback ->
                    val cb = callback as? NsdManager.ServiceInfoCallback
                    if (cb != null) {
                        try {
                            nsdManager.unregisterServiceInfoCallback(cb)
                        } catch (e: Exception) {
                            log.e(e) { "Error unregistering callback" }
                        }
                    }
                }
                activeCallbacks.clear()
            }
        }
    }.shareIn(
        scope = applicationScope,
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

    private fun String.sanitizeHost(): String {
        val result = if (this.startsWith("/")) this.substring(1) else this
        log.i { "sanitizeHost input $this output $result" }
        return result
    }

    /**
     * NsdManager exposes TXT records as raw bytes; decode them so the identity the server
     * advertises is available without an HTTP round trip.
     */
    private fun NsdServiceInfo.toDiscoveredServer(host: String): DiscoveredServer {
        val properties = attributes.mapValues { (_, value) ->
            value?.toString(Charsets.UTF_8)
        }
        val advertisement = ServerTxtRecord.decode(properties)
        return DiscoveredServer(
            name = serviceName,
            ipAddress = host,
            port = port,
            serverId = advertisement?.serverId,
            scheme = advertisement?.scheme ?: ServerTxtRecord.SCHEME_HTTP,
            alternateAddresses = advertisement?.addresses.orEmpty().filterNot { it == host }
        )
    }
}