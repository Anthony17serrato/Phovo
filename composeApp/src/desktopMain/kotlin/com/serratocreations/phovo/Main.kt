package com.serratocreations.phovo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.serratocreations.phovo.di.initKoin
import com.serratocreations.phovo.ui.PhovoApp
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    // TODO: Move to data layer
    GlobalScope.launch {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    }
    Window(onCloseRequest = ::exitApplication, title = "Phovo") {
        initKoin()
        PhovoApp()
    }
}

fun Application.module() {
    configureRouting()
}