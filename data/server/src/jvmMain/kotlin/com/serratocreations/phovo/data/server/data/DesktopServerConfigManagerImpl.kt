package com.serratocreations.phovo.data.server.data

import com.serratocreations.phovo.core.common.PART_EXTENSION
import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.UploadInitResponse
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.DesktopServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerEventsRepository
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
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
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.net.NetworkInterface
import java.net.Inet4Address
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
                val mediaItemDto = call.receive<MediaItemDto>()
                // TODO If file exists but is partial, delete file and allow re-upload
                // TODO if filename exist but asset hash is different, append a _n to the filename
                if (localMediaRepository.doesAssetExist(mediaItemDto.assetHash)) {
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
                    localUri = filePath.absolutePath()
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
                embeddedServer(factory = Netty, port = 8080, host = "0.0.0.0", module = routingConfig)
                    .start(wait = false)
                serverConfigState.update {
                    it.copy(configStatus = ConfigStatus.Configured(
                        serverUrl = "http://${getHostIPv4()}:8080"
                    ))
                }
            }
        }
    }
}