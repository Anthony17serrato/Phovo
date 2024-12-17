package com.serratocreations.phovo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.serratocreations.phovo.di.initKoin
import com.serratocreations.phovo.ui.PhovoApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Phovo") {
        initKoin()
        PhovoApp()
    }
}