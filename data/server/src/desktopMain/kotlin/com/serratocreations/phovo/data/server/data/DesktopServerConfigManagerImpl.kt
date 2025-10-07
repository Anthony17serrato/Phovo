package com.serratocreations.phovo.data.server.data

import com.serratocreations.phovo.core.database.entities.MediaItemUriEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithUriEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.mappers.toMediaItem
import com.serratocreations.phovo.data.photos.mappers.toMediaItemDto
import com.serratocreations.phovo.data.photos.mappers.toMediaItemEntity
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerEventsRepository
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
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.net.NetworkInterface
import java.net.Inet4Address
import java.net.URI
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DesktopServerConfigManagerImpl(
    logger: PhovoLogger,
    private val serverConfigRepository: DesktopServerConfigRepository,
    private val serverEventsRepository: ServerEventsRepository,
    private val localSupportMediaRepository: LocalSupportMediaRepository,
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
): DesktopServerConfigManager {
    // Caches the current config state for new subscribers
    private val serverConfigState = MutableStateFlow(ServerConfigState())
    private val log = logger.withTag("DesktopServerConfigManagerImpl")

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

            // Upload initialization – send JSON metadata once
            post("/upload/init") {
                val mediaItemDto = call.receive<MediaItemDto>() // your data class with name, size, etc.
                val directory = serverConfigRepository.observeServerConfig().first()
                    ?.backupDirectory?.plus("/") ?: error("No server config")

                val dirPath = Paths.get(directory)
                if (!Files.exists(dirPath)) Files.createDirectories(dirPath)
                val filePath = dirPath.resolve(mediaItemDto.fileName + ".part")

                // Create or truncate file to start fresh
                Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { }

                val mediaItemWithUriEntity = MediaItemWithUriEntity(
                    mediaItemEntity = mediaItemDto.toMediaItemEntity(),
                    mediaItemUri = MediaItemUriEntity(
                        mediaUuid = mediaItemDto.localUuid,
                        uri = filePath.toUri().toString()
                    )
                )

                this@DesktopServerConfigManagerImpl.log.i { "Initialized upload for ${mediaItemDto.fileName}" }
                localSupportMediaRepository.addOrUpdateMediaItem(mediaItemWithUriEntity)
                call.respond(HttpStatusCode.Created, "Upload initialized")
            }

            // Chunk appending – raw bytes
            post("/upload/chunk") {
                val fileName = call.request.headers["X-File-Name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val chunkIndex = call.request.headers["X-Chunk-Index"]?.toIntOrNull() ?: 0
                val totalChunks = call.request.headers["X-Chunk-Total"]?.toIntOrNull()

                val directory = serverConfigRepository.observeServerConfig().first()
                    ?.backupDirectory?.plus("/") ?: error("No server config")

                val filePath = Paths.get(directory, "$fileName.part")

                FileOutputStream(filePath.toFile(), true).use { fos ->
                    call.receiveChannel().copyTo(fos)
                }

                this@DesktopServerConfigManagerImpl.log.i { "Appended chunk $chunkIndex/$totalChunks to $fileName (${Files.size(filePath)} bytes so far)" }

                call.respond(HttpStatusCode.Created, "Chunk uploaded")
            }

            // Finalize upload – rename .part → real file
            post("/upload/complete") {
                val localUuid = call.receiveText() // client just sends file uuid
                var mediaItemWithUriEntity = localSupportMediaRepository.getMediaItemByLocalUuid(localUuid)
                    ?: run {
                        call.respond(HttpStatusCode.NotFound, "Server is missing" +
                                "a record for the provided uuid.")
                        return@post
                    }
                val uri = URI.create(mediaItemWithUriEntity.mediaItemUri.uri)
                val filePath = Paths.get(uri)
                val finalPath = filePath.parent.resolve(mediaItemWithUriEntity.mediaItemEntity.fileName)

                Files.move(filePath, finalPath, StandardCopyOption.REPLACE_EXISTING)

                this@DesktopServerConfigManagerImpl.log.i { "Upload complete for $localUuid" }
                mediaItemWithUriEntity = mediaItemWithUriEntity.copy(
                    mediaItemEntity = mediaItemWithUriEntity.mediaItemEntity.copy(remoteUuid = Uuid.random().toString()),
                    mediaItemUri = mediaItemWithUriEntity.mediaItemUri.copy(uri = finalPath.toUri().toString()),
                )
                localSupportMediaRepository.addOrUpdateMediaItem(mediaItemWithUriEntity.toMediaItem())
                val response = mediaItemWithUriEntity.toMediaItemDto()
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }

    override fun observeDeviceServerConfigurationState(scope: CoroutineScope): Flow<ServerConfigState> {
        scope.observeDeviceServerConfigurationState()
        return serverConfigState.asStateFlow()
    }

    private fun CoroutineScope.observeDeviceServerConfigurationState() {
        // TODO: fetch the initial server state from room
        serverConfigState.update { it.copy(configStatus = ConfigStatus.NotConfigured) }
        serverEventsRepository.serverEventLogsFlow().onEach { logs ->
            serverConfigState.update { it.copy(serverEventLogs = logs) }
        }.launchIn(this)
    }

    override fun configureDeviceAsServer(serverConfig: ServerConfig) {
        appScope.launch {
            log.i { "configureDeviceAsServer $serverConfig" }
            serverConfigRepository.updateServerConfig(serverConfig)
            serverConfigState.update {
                it.copy(configStatus = ConfigStatus.NotConfigured)
            }
            launch(ioDispatcher) {
                embeddedServer(factory = Netty, port = 8080, host = "0.0.0.0", module = routingConfig)
                    .start(wait = false)
                serverConfigState.update {
                    it.copy(configStatus = ConfigStatus.Configured(
                        serverState = ServerState.Online,
                        serverUrl = "http://${getHostIPv4()}:8080"
                    ))
                }
            }
            // TODO Save server config to room db
        }
    }
}