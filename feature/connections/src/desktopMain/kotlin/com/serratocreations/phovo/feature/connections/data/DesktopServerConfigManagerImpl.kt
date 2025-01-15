package com.serratocreations.phovo.feature.connections.data

import com.serratocreations.phovo.data.photos.repository.PhovoItemRepository
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

class DesktopServerConfigManagerImpl(
    private val phovoItemRepository: PhovoItemRepository,
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
): DesktopServerConfigManager {
    // Caches the current config state for new subscribers
    private val serverConfigState = MutableStateFlow(ServerConfigState())

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
                phovoItemRepository.addServerEventLog("get ${LocalDateTime.now()}")
                call.respond(HttpStatusCode.OK, "Phovo server is running")
            }

            post("/upload") {
//                // Receive the photo data as a JSON object
//                val photo = call.receive<PhovoImageItem>()
//
//                // Log received data
//                phovoItemRepository.addServerEventLog("upload photo $photo")
//
//                // Respond with a success message
//                call.respond(HttpStatusCode.Created, "Photo uploaded successfully")

                println("reached upload api")
                // Receive the uploaded file
                val multipart = call.receiveMultipart()
                var fileName: String? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        fileName = part.originalFileName
                        println("upload filename $fileName")
                        val fileBytes = part.streamProvider().readBytes()

                        val file = File("uploads/${fileName ?: "unknown.jpg"}")
                        if (!file.parentFile.exists()) {
                            file.parentFile.mkdirs()  // Create the necessary directories
                        }

                        try {
                            file.writeBytes(fileBytes)
                        } catch (e: Exception) {
                            println("Failed to save file: ${e.message}")
                        }

                        phovoItemRepository.addServerEventLog("File uploaded ${file.absolutePath}")
                    }
                    part.dispose()
                }

                // Respond with success
                if (fileName != null) {
                    call.respond(HttpStatusCode.Created, "File uploaded successfully: $fileName")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "No file uploaded")
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
        phovoItemRepository.serverEventLogsFlow().onEach { logs ->
            serverConfigState.update { it.copy(serverEventLogs = logs) }
        }.launchIn(this)
    }

    override fun configureDeviceAsServer() {
        appScope.launch {
            serverConfigState.update {
                it.copy(configStatus = ConfigStatus.NotConfigured)
            }
            launch(ioDispatcher) {
                embeddedServer(factory = Netty, port = 8080, host = "0.0.0.0", module = routingConfig)
                    .start(wait = false)
                serverConfigState.update {
                    it.copy(configStatus = ConfigStatus.Configured(ServerState.Online))
                }
            }
            // TODO Save server config to room db
        }
    }
}