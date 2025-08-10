package com.serratocreations.phovo.feature.photos.ui.reusablecomponents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import coil3.Uri
import java.awt.Desktop
import java.io.File

@Composable
actual fun VideoPlayer(videoUri: Uri, modifier: Modifier) {
    LaunchedEffect(videoUri) {
        val path = videoUri.path ?: return@LaunchedEffect
        val file = File(path)

        if (Desktop.isDesktopSupported() && file.exists()) {
            try {
                Desktop.getDesktop().open(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}