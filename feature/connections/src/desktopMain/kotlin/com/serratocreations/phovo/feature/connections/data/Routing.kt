package com.serratocreations.phovo.feature.connections.data

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.receive
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
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

@Serializable
data class Photo(val uri: String, val dateTaken: String) // Placeholder for your photo data class
