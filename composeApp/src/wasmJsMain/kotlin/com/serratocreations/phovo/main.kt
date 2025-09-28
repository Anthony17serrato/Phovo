package com.serratocreations.phovo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.serratocreations.phovo.di.initApplication
import com.serratocreations.phovo.ui.PhovoApp
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initApplication()
    ComposeViewport(document.body!!) {
        PhovoApp()
    }
}