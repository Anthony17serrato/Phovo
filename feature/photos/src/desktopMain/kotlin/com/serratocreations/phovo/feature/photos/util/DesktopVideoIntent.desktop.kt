package com.serratocreations.phovo.feature.photos.util

import coil3.Uri
import java.awt.Desktop
import java.io.File

actual fun handleVideoDesktop(uri: Uri) {
    val path = uri.path ?: return
    val file = File(path)

    if (Desktop.isDesktopSupported() && file.exists()) {
        try {
            Desktop.getDesktop().open(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}