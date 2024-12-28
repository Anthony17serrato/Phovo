package com.serratocreations.phovo.feature.connections.data

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DesktopServerConfigManagerImpl(): DesktopServerConfigManager {
    // Caches the current config state for new subscribers
    private val serverState = MutableSharedFlow<ConfigStatus>(replay = 1)

    init {
        // TODO: fetch the initial server state from room
        serverState.tryEmit(ConfigStatus.NotConfigured)
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
                println("get")
                call.respond(HttpStatusCode.OK, "Phovo server is running")
            }

            post("/upload") {
                // Receive the photo data as a JSON object
                val photo = call.receive<Photo>()

                // Log received data
                println("Received photo URI: ${photo.uri} and Date Taken: ${photo.dateTaken}")

                // Respond with a success message
                call.respond(HttpStatusCode.Created, "Photo uploaded successfully")
            }
        }
    }

    override fun observeDeviceServerConfigurationState(): Flow<ConfigStatus> {
        return serverState.asSharedFlow()
    }

    // TODO: Configure and use application scope
    override fun configureDeviceAsServer() {
        GlobalScope.launch {
            serverState.emit(ConfigStatus.NotConfigured)
            launch {
                embeddedServer(factory = Netty, port = 8080, host = "0.0.0.0", module = routingConfig)
                    .start(wait = false)
                serverState.emit(ConfigStatus.Configured(ServerState.Online))
            }
            // TODO Save server config to room db
        }
    }
}

@Serializable
data class Photo(val uri: String, val dateTaken: String) // Placeholder for your photo data class