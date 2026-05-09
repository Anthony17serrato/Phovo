package com.serratocreations.phovo.feature.photos.util

import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import io.github.vinceglb.filekit.exists
import java.awt.Desktop

// TODO Use instead https://github.com/kdroidFilter/ComposeMediaPlayer
actual fun handleVideoDesktop(assetLocation: DomainAssetLocation) {
    val file = if (assetLocation is DomainAssetLocation.LocalAssetLocation) assetLocation.localAssetLocation else return

    if (Desktop.isDesktopSupported() && file.exists()) {
        try {
            Desktop.getDesktop().open(file.file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}