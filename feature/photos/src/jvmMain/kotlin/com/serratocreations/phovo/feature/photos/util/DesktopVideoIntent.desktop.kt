package com.serratocreations.phovo.feature.photos.util

import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
import io.github.vinceglb.filekit.exists
import java.awt.Desktop

// TODO Use instead https://github.com/kdroidFilter/ComposeMediaPlayer
actual fun handleVideoDesktop(localOrRemoteAsset: LocalOrRemoteAsset) {
    val file = if (localOrRemoteAsset is LocalOrRemoteAsset.LocalAsset) localOrRemoteAsset.localAssetLocation else return

    if (Desktop.isDesktopSupported() && file.exists()) {
        try {
            Desktop.getDesktop().open(file.file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}