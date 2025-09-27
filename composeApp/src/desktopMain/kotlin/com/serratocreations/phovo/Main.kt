package com.serratocreations.phovo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.serratocreations.phovo.di.initApplication
import com.serratocreations.phovo.ui.PhovoApp

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Phovo",
        state = windowState
    ) {
        initApplication()
        PhovoApp()
    }
}