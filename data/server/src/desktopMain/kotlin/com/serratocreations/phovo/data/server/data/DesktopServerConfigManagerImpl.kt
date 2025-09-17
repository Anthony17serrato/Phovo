package com.serratocreations.phovo.data.server.data

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerEventsRepository
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
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

class DesktopServerConfigManagerImpl(
    logger: PhovoLogger,
    private val serverConfigRepository: DesktopServerConfigRepository,
    private val serverEventsRepository: ServerEventsRepository,
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

            post("/upload") {
                this@DesktopServerConfigManagerImpl.log.i { "Reached upload API" }

                val multipart = call.receiveMultipart()
                var exception: Exception? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        try {
                            val directory = serverConfigRepository.observeServerConfig().first()
                                ?.backupDirectory?.plus("/") ?: run {
                                this@DesktopServerConfigManagerImpl.log.i { "Server config is null" }
                                return@forEachPart
                            }

                            val dirPath = Paths.get(directory)
                            if (!Files.exists(dirPath)) Files.createDirectories(dirPath)

                            val originalFileName = part.originalFileName ?: "unknown"
                            val chunkIndex = part.headers["X-Chunk-Index"]?.toIntOrNull() ?: 0

                            val filePath = dirPath.resolve(originalFileName)

                            // Append to file instead of overwriting
                            FileOutputStream(filePath.toFile(), true).use { fos ->
                                part.streamProvider().use { input ->
                                    fos.write(input.readAllBytes())
                                }
                            }

                            this@DesktopServerConfigManagerImpl.log.i {
                                "Appended chunk $chunkIndex to $originalFileName (${Files.size(filePath)} bytes so far)"
                            }

                        } catch (e: Exception) {
                            if (e is CancellationException) throw e else exception = e
                        }
                    }
                    part.dispose()
                }

                if (exception == null) {
                    call.respond(HttpStatusCode.Created, "File chunk uploaded successfully")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to upload chunk: $exception")
                }
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