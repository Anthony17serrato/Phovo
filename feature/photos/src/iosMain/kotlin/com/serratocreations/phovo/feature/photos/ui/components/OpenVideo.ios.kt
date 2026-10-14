package com.serratocreations.phovo.feature.photos.ui.components

import com.serratocreations.phovo.core.common.util.toPhAsset
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsVersion
import platform.Photos.PHVideoRequestOptionsVersionCurrent
import platform.Photos.PHVideoRequestOptionsVersionOriginal
import kotlin.coroutines.resume

/**
 * Videos on iOS are photo library assets rather than files, so the player is handed the URL of a
 * file backing the asset.
 *
 * The current version is preferred, so edits made in Photos, such as a rotation, show up in the
 * player the same as in the thumbnail. A slow motion video's current version is an AVComposition
 * with no file behind it though, so that falls back to the original.
 */
internal actual suspend fun VideoPlayerState.openVideo(file: PlatformFile) {
    val asset = file.toPhAsset() ?: return
    val url = asset.requestVideoUrl(PHVideoRequestOptionsVersionCurrent)
        ?: asset.requestVideoUrl(PHVideoRequestOptionsVersionOriginal)
        ?: return
    val uri = url.absoluteString ?: return
    openUri(uri)
}

private suspend fun PHAsset.requestVideoUrl(version: PHVideoRequestOptionsVersion): NSURL? =
    suspendCancellableCoroutine { continuation ->
        val options = PHVideoRequestOptions().apply {
            this.version = version
            // Lets videos that only exist in iCloud download before they play.
            networkAccessAllowed = true
        }
        val manager = PHImageManager.defaultManager()
        val requestId = manager.requestAVAssetForVideo(this, options) { avAsset, _, _ ->
            continuation.resume((avAsset as? AVURLAsset)?.URL)
        }
        continuation.invokeOnCancellation { manager.cancelImageRequest(requestId) }
    }
