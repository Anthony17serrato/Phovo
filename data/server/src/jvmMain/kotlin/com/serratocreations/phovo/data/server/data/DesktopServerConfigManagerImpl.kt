package com.serratocreations.phovo.data.server.data

import com.serratocreations.phovo.core.common.HIGH_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.PART_EXTENSION
import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.UploadInitResponse
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.ServerHealth
import com.serratocreations.phovo.core.model.network.ServerTxtRecord
import com.serratocreations.phovo.core.model.network.ApiEndpoints.GET_ALL_MEDIA_API
import com.serratocreations.phovo.core.model.network.ApiEndpoints.HIGH_RES_THUMBNAIL_API
import com.serratocreations.phovo.core.model.network.ApiEndpoints.LOW_RES_THUMBNAIL_API
import com.serratocreations.phovo.core.model.network.ApiEndpoints.SOURCE_FILE_API
import com.serratocreations.phovo.data.photos.mappers.toMediaItemDto
import com.serratocreations.phovo.core.serverconfig.DesktopServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerEventsRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.net.NetworkInterface
import java.net.Inet4Address
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.uuid.ExperimentalUuidApi

class DesktopServerConfigManagerImpl(
    logger: PhovoLogger,
    private val serverConfigRepository: DesktopServerConfigRepository,
    private val serverEventsRepository: ServerEventsRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
): DesktopServerConfigManager {
    // Caches the current config state for new subscribers
    private val serverConfigState = MutableStateFlow(ServerConfigState())
    private val log = logger.withTag("DesktopServerConfigManagerImpl")
    private var jmdns: JmDNS? = null

    /**
     * Held so the engine can be released before rebinding on reconfigure, and shut down cleanly on
     * exit. Only touched from the ioDispatcher launch in [configureDeviceAsServer].
     */
    private var server: EmbeddedServer<*, *>? = null

    private companion object {
        const val SERVER_PORT = 8080
        const val SERVER_STOP_GRACE_MILLIS = 1_000L
        const val SERVER_STOP_TIMEOUT_MILLIS = 5_000L

        // Only one instance is ever advertised, so the DNS-SD tie breakers are not meaningful.
        const val SERVICE_WEIGHT = 0
        const val SERVICE_PRIORITY = 0

        /**
         * Interface name prefixes used by hypervisors and container runtimes for host-only
         * networks. Addresses on these are reachable from this machine only.
         */
        val HYPERVISOR_INTERFACE_PREFIXES = listOf(
            "bridge",   // Parallels Desktop on macOS
            "vmnet",    // VMware
            "vboxnet",  // VirtualBox
            "docker",   // Docker
            "br-",      // Docker user defined bridges
            "virbr",    // libvirt
            "utun",     // macOS tunnels/VPNs
            "tun",
            "tap"
        )
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                jmdns?.unregisterAllServices()
                jmdns?.close()
            } catch (e: Exception) {
                System.err.println("Error shutting down JmDNS: ${e.message}")
            }
            try {
                server?.stop(
                    gracePeriodMillis = SERVER_STOP_GRACE_MILLIS,
                    timeoutMillis = SERVER_STOP_TIMEOUT_MILLIS
                )
            } catch (e: Exception) {
                System.err.println("Error shutting down Ktor server: ${e.message}")
            }
        })
    }

    /**
     * Returns the IPv4 address clients on the LAN can actually reach this machine at.
     *
     * Interface enumeration order is not meaningful, and hypervisor bridges (Parallels, VMware,
     * Docker, VirtualBox) hand out private addresses that look identical to a real LAN address
     * while being reachable only from this host. [NetworkInterface.isVirtual] does not identify
     * them — it only reports subinterfaces such as `en0:1`. Picking one of those silently
     * configures every client with an unroutable server URL.
     *
     * The address the OS would use to leave this machine is the one clients share a network with,
     * so that is preferred. Opening an unconnected UDP socket resolves the route without sending
     * any packets. The interface scan is the fallback for when there is no default route, or when
     * the route leaves through a tunnel that LAN clients are not on.
     */
    private fun getHostIPv4(): String {
        val candidates = lanCandidateAddresses()
        val defaultRouteAddress = defaultRouteIPv4()

        log.i {
            "Host IPv4 candidates: ${candidates.map { it.hostAddress }}, " +
                "default route: ${defaultRouteAddress?.hostAddress}"
        }

        // The routed address is authoritative when it also survived the interface scan.
        if (defaultRouteAddress != null && defaultRouteAddress in candidates) {
            return defaultRouteAddress.hostAddress
        }

        // Otherwise the route leaves through an excluded interface — a VPN tunnel, say — so fall
        // back to a private address a client on the same LAN can route to.
        return candidates.firstOrNull { it.isSiteLocalAddress }?.hostAddress
            ?: defaultRouteAddress?.hostAddress
            ?: candidates.firstOrNull()?.hostAddress
            ?: "127.0.0.1"
    }

    private fun defaultRouteIPv4(): Inet4Address? = try {
        java.net.DatagramSocket().use { socket ->
            // Connecting a UDP socket only selects a route, no traffic is sent.
            socket.connect(InetAddress.getByName("1.1.1.1"), 53)
            (socket.localAddress as? Inet4Address)?.takeIf { it.isUsableHostAddress() }
        }
    } catch (e: Exception) {
        log.e(e) { "Unable to resolve default route address" }
        null
    }

    private fun lanCandidateAddresses(): List<Inet4Address> = try {
        java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint && it.supportsMulticast() }
            .filterNot { it.name.isHypervisorInterfaceName() }
            .flatMap { ni -> java.util.Collections.list(ni.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .filter { it.isUsableHostAddress() }
    } catch (e: Exception) {
        log.e(e) { "Unable to enumerate network interfaces" }
        emptyList()
    }

    /** Excludes the wildcard, loopback and self-assigned (169.254.0.0/16) addresses. */
    private fun Inet4Address.isUsableHostAddress(): Boolean =
        !isAnyLocalAddress && !isLoopbackAddress && !isLinkLocalAddress

    private fun String.isHypervisorInterfaceName(): Boolean {
        val name = lowercase()
        return HYPERVISOR_INTERFACE_PREFIXES.any { name.startsWith(it) }
    }

    override fun getDefaultServerName(): String {
        val rawName = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME") ?: "Phovo Server"
        }
        val cleanName = rawName.substringBefore(".")
        return if (cleanName.isEmpty() || cleanName.matches(Regex("\\d+"))) {
            "Phovo Server"
        } else {
            cleanName
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private val routingConfig: Application.() -> Unit = {
        install(StatusPages) {
            exception<IllegalStateException> { call, cause ->
                call.respondText("App in illegal state as ${cause.message}")
            }
        }
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true })
        }

        routing {
            staticResources("/content", "mycontent")

            get("/") {
                serverEventsRepository.addServerEventLog("get ${LocalDateTime.now()}")
                call.respond(HttpStatusCode.OK, "Phovo server is running")
            }

            // Liveness plus identity. Clients use serverId to confirm they are still talking to
            // the server they paired with, rather than to whoever now holds that address.
            get("/${ApiEndpoints.HEALTH_API.value}") {
                val config = serverConfigRepository.observeServerConfig().first()
                val serverId = serverConfigRepository.serverId()
                if (config == null || serverId == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Server is not configured")
                    return@get
                }
                call.respond(
                    HttpStatusCode.OK,
                    ServerHealth(serverId = serverId, serverName = config.serverName)
                )
            }

            get("/${GET_ALL_MEDIA_API.value}") {
                val mediaItems = localMediaRepository.phovoMediaFlow().first()
                val mediaItemDtos = mediaItems.map { it.toMediaItemDto() }
                serverEventsRepository.addServerEventLog("GET_MEDIA_API ${LocalDateTime.now()}")
                call.respond(HttpStatusCode.OK, mediaItemDtos)
            }

            // --- THUMBNAILS API ---
            get("/${LOW_RES_THUMBNAIL_API.value}{hash}") {
                val hash = call.parameters["hash"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing hash parameter")

                val directory = serverConfigRepository.observeServerConfig().first()
                    ?.backupDirectory ?: return@get call.respond(HttpStatusCode.InternalServerError, "No server config")

                val thumbnailFile =
                    File(directory.absolutePath(), "$LOW_RES_THUMBNAIL_DIR/$hash.webp")

                if (thumbnailFile.exists()) {
                    call.respondFile(thumbnailFile)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Low-res thumbnail not found")
                }
            }

            get("/${HIGH_RES_THUMBNAIL_API.value}{hash}") {
                val hash = call.parameters["hash"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing hash parameter")

                val directory = serverConfigRepository.observeServerConfig().first()
                    ?.backupDirectory ?: return@get call.respond(HttpStatusCode.InternalServerError, "No server config")

                val thumbnailFile = File(directory.absolutePath(), "$HIGH_RES_THUMBNAIL_DIR/$hash.webp")

                if (thumbnailFile.exists()) {
                    call.respondFile(thumbnailFile)
                } else {
                    call.respond(HttpStatusCode.NotFound, "High-res thumbnail not found")
                }
            }

            get("/${SOURCE_FILE_API.value}{hash}") {
                val hash = call.parameters["hash"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing hash parameter")

                val localItem = localMediaRepository.getLocalMediaByAssetHash(hash)
                    ?: return@get call.respond(HttpStatusCode.InternalServerError, "Asset not found locally")

                val thumbnailFile = PlatformFile(localItem.localUri)

                if (thumbnailFile.exists()) {
                    call.respondFile(thumbnailFile.file)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Source file not found")
                }
            }

            // Upload initialization – send JSON metadata once
            post("/upload/init") {
                val mediaItemDto = call.receive<MediaItemDto>()
                // TODO If file exists but is partial, delete file and allow re-upload
                // TODO if filename exist but asset hash is different, append a _n to the filename
                if (localMediaRepository.doesCompleteAssetExist(mediaItemDto.assetHash)) {
                    call.respond(
                        HttpStatusCode.OK,
                        UploadInitResponse(
                            uploadRequired = false,
                            message = "Asset already exists"
                        )
                    )
                    return@post
                }

                val directory = serverConfigRepository.observeServerConfig().first()
                    ?.backupDirectory ?: error("No Server Config")

                directory.createDirectories(mustCreate = false)

                val filePath = directory / (mediaItemDto.fileName + PART_EXTENSION)

                Files.newOutputStream(
                    filePath.file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { }

                val localMediaEntity = LocalMediaEntity(
                    assetHash = mediaItemDto.assetHash,
                    localUri = filePath.absolutePath(),
                    isPartial = true
                )

                localMediaRepository.addOrUpdateLocalMediaItem(localMediaEntity)

                call.respond(
                    HttpStatusCode.Created,
                    UploadInitResponse(
                        uploadRequired = true,
                        message = "Upload initialized"
                    )
                )
            }

            // Chunk appending – raw bytes
            post("/upload/chunk") {
                // TODO Use Asset Hash instead of file name
                val fileName = call.request.headers["X-File-Name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val chunkIndex = call.request.headers["X-Chunk-Index"]?.toIntOrNull() ?: 0
                val totalChunks = call.request.headers["X-Chunk-Total"]?.toIntOrNull()
                // TODO Migrate to FileKit
                val directory = serverConfigRepository.observeServerConfig().first()
                    ?.backupDirectory?.absolutePath()?.plus("/") ?: error("No server config")

                val filePath = Paths.get(directory, "$fileName$PART_EXTENSION")
                call.receiveChannel().copyAndClose(filePath.toFile().writeChannel())

                this@DesktopServerConfigManagerImpl.log.i { "Appended chunk $chunkIndex/$totalChunks to $fileName (${Files.size(filePath)} bytes so far)" }

                call.respond(HttpStatusCode.Created, "Chunk uploaded")
            }

            // Finalize upload – rename .part → real file
            post("/upload/complete") {
                // TODO verify final file matches the asset hash
                val localUuid = call.receiveText() // client just sends file uuid
                var localMediaEntity = localMediaRepository.getLocalMediaByAssetHash(localUuid)
                    ?: run {
                        call.respond(HttpStatusCode.NotFound, "Server is missing" +
                                " a record for the provided uuid.")
                        return@post
                    }

                suspend fun moveFileToFinalPath(): String = withContext(ioDispatcher) {
                    try {
                        val filePath = Paths.get(localMediaEntity.localUri)

                        val finalPath = filePath.parent.resolve(
                            filePath.fileName.toString().removeSuffix(PART_EXTENSION)
                        )

                        Files.move(filePath, finalPath, StandardCopyOption.REPLACE_EXISTING)

                        finalPath.toAbsolutePath().toString()
                    } catch (e: Exception) {
                        this@DesktopServerConfigManagerImpl.log.e(e) { "Exception moving file" }
                        throw e
                    }
                }
                this@DesktopServerConfigManagerImpl.log.i { "Upload complete for $localUuid" }
                localMediaEntity = localMediaEntity.copy(
                    localUri = moveFileToFinalPath(),
                    isPartial = false
                )
                localMediaRepository.addOrUpdateLocalMediaItem(localMediaEntity)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    override fun observeDeviceServerConfigurationState(scope: CoroutineScope): Flow<ServerConfigState> {
        scope.observeDeviceServerConfigurationState()
        return serverConfigState.asStateFlow()
    }

    private fun CoroutineScope.observeDeviceServerConfigurationState() {
        serverEventsRepository.serverEventLogsFlow().onEach { logs ->
            serverConfigState.update { it.copy(serverEventLogs = logs) }
        }.launchIn(this)
    }

    override fun configureDeviceAsServer(serverConfig: ServerConfig.ServerSpecificServerConfig) {
        appScope.launch {
            log.i { "configureDeviceAsServer $serverConfig" }
            serverConfigRepository.updateServerConfig(serverConfig)
            serverConfigState.update {
                it.copy(
                    configStatus = ConfigStatus.Starting
                )
            }
            launch(ioDispatcher) {
                try {
                    jmdns?.unregisterAllServices()
                    jmdns?.close()
                } catch (e: Exception) {
                    log.e(e) { "Error closing existing JmDNS" }
                }
                jmdns = null

                // Reconfiguring is a supported action (this is called from both DesktopAppInitializer
                // on startup and the connections UI), so the previous engine has to be released
                // before rebinding the port — otherwise the second call fails with a BindException.
                try {
                    server?.stop(
                        gracePeriodMillis = SERVER_STOP_GRACE_MILLIS,
                        timeoutMillis = SERVER_STOP_TIMEOUT_MILLIS
                    )
                } catch (e: Exception) {
                    log.e(e) { "Error stopping existing Ktor server" }
                }
                server = null

                server = try {
                    embeddedServer(
                        factory = Netty,
                        port = SERVER_PORT,
                        host = "0.0.0.0",
                        module = routingConfig
                    ).also { it.start(wait = false) }
                } catch (e: Exception) {
                    log.e(e) { "Failed to start Ktor server on port $SERVER_PORT" }
                    serverConfigState.update { it.copy(configStatus = ConfigStatus.NotConfigured) }
                    return@launch
                }

                val hostIp = getHostIPv4()
                // Written by updateServerConfig above, so this is only null if that failed.
                val serverId = serverConfigRepository.serverId()
                if (serverId == null) {
                    log.e { "Server id missing after configuration, skipping mDNS advertisement" }
                    serverConfigState.update {
                        it.copy(configStatus = ConfigStatus.Configured("http://$hostIp:$SERVER_PORT"))
                    }
                    return@launch
                }
                log.i { "Starting JmDNS advertisement for server IP: $hostIp id: $serverId" }
                try {
                    val inetAddress = InetAddress.getByName(hostIp)
                    val sanitizedName = serverConfig.serverName.replace(".", " ")
                    // Advertise every address this host believes it is reachable on, with the
                    // routable one first, so a client that cannot reach the primary has somewhere
                    // else to try. A single JmDNS instance is used deliberately: one instance per
                    // interface makes jmdns rename the service ("name (2)"), and both clients key
                    // their discovery map on the service name.
                    val advertisedAddresses = (listOf(hostIp) + lanCandidateAddresses()
                        .map { it.hostAddress })
                        .distinct()
                    val txtRecord = ServerTxtRecord.encode(
                        serverId = serverId,
                        serverName = sanitizedName,
                        addresses = advertisedAddresses
                    )
                    jmdns = JmDNS.create(inetAddress, "PhovoServer").apply {
                        val serviceInfo = ServiceInfo.create(
                            "_phovo._tcp.local.",
                            sanitizedName,
                            SERVER_PORT,
                            SERVICE_WEIGHT,
                            SERVICE_PRIORITY,
                            txtRecord
                        )
                        registerService(serviceInfo)
                    }
                    log.i { "JmDNS service registered successfully with TXT $txtRecord" }
                } catch (e: Exception) {
                    log.e(e) { "Failed to start JmDNS service advertising" }
                }

                serverConfigState.update {
                    it.copy(configStatus = ConfigStatus.Configured(
                        serverUrl = "http://$hostIp:$SERVER_PORT"
                    ))
                }
            }
        }
    }
}