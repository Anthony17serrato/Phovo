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

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                jmdns?.unregisterAllServices()
                jmdns?.close()
            } catch (e: Exception) {
                System.err.println("Error shutting down JmDNS: ${e.message}")
            }
        })
    }

    private fun getHostIPv4(): String {
        return try {
            val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            val candidates = interfaces
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { ni -> java.util.Collections.list(ni.inetAddresses) }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress }
            candidates.firstOrNull { address ->
                address.startsWith("192.") || address.startsWith("10.") || address.startsWith("172.")
            } ?: candidates.firstOrNull() ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
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

                embeddedServer(factory = Netty, port = 8080, host = "0.0.0.0", module = routingConfig)
                    .start(wait = false)

                val hostIp = getHostIPv4()
                log.i { "Starting JmDNS advertisement for server IP: $hostIp" }
                try {
                    val inetAddress = InetAddress.getByName(hostIp)
                    val sanitizedName = serverConfig.serverName.replace(".", " ")
                    jmdns = JmDNS.create(inetAddress, "PhovoServer").apply {
                        val serviceInfo = ServiceInfo.create(
                            "_phovo._tcp.local.",
                            sanitizedName,
                            8080,
                            "Phovo Photo Backup Server"
                        )
                        registerService(serviceInfo)
                    }
                    log.i { "JmDNS service registered successfully" }
                } catch (e: Exception) {
                    log.e(e) { "Failed to start JmDNS service advertising" }
                }

                serverConfigState.update {
                    it.copy(configStatus = ConfigStatus.Configured(
                        serverUrl = "http://$hostIp:8080"
                    ))
                }
            }
        }
    }
}