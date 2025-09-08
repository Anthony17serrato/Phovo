package com.serratocreations.phovo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.serratocreations.phovo.di.initKoin
import com.serratocreations.phovo.ui.PhovoApp
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(document.body!!) {
        PhovoApp()
    }
}