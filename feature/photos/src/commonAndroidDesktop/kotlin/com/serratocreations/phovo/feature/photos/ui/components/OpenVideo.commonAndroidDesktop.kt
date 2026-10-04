package com.serratocreations.phovo.feature.photos.ui.components

import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.vinceglb.filekit.PlatformFile

internal actual suspend fun VideoPlayerState.openVideo(file: PlatformFile): Float? {
    openFile(file)
    return null
}
