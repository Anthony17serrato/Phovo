package com.serratocreations.kanbanboard

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.serratocreations.kanbanboard.ui.PhovoApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kanban Board") {
        PhovoApp()
    }
}