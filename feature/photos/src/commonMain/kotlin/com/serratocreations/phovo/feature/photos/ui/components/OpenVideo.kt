package com.serratocreations.phovo.feature.photos.ui.components

import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.vinceglb.filekit.PlatformFile

/**
 * Opens [file] in this player and starts playback. Each platform stores videos differently, so
 * turning a [PlatformFile] into something the player can read is platform specific.
 */
internal expect suspend fun VideoPlayerState.openVideo(file: PlatformFile)
