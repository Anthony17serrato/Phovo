package com.serratocreations.phovo.feature.photos.ui.components

import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.vinceglb.filekit.PlatformFile

/**
 * Opens [source] in this player and starts playback. A video that is only on the server is
 * streamed from its source file URL, which the server answers in byte ranges.
 */
internal suspend fun VideoPlayerState.openVideo(source: DomainAssetLocation) {
    when (source) {
        is DomainAssetLocation.LocalAssetLocation -> openLocalVideo(source.localAssetLocation)
        is DomainAssetLocation.RemoteAssetLocation -> openUri(source.remoteAssetUri.toString())
    }
}

/**
 * Opens a video stored on this device. Each platform stores videos differently, so turning a
 * [PlatformFile] into something the player can read is platform specific.
 */
internal expect suspend fun VideoPlayerState.openLocalVideo(file: PlatformFile)
