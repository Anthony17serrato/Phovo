package com.serratocreations.phovo.feature.photos.ui.reusablecomponents

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import coil3.Uri
import com.serratocreations.phovo.core.common.util.localIdFromPhAssetUri
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVKit.AVPlayerViewController
import platform.Photos.PHAsset
import platform.Photos.PHImageManager

@Composable
actual fun VideoPlayer(videoUri: Uri, modifier: Modifier) {
    var player: AVPlayer? by remember { mutableStateOf(null) }

    UIKitViewController(
        factory = {
            val localId = localIdFromPhAssetUri(videoUri)
            val assets = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localId), null)
            val asset = assets.firstObject as? PHAsset

            val controller = AVPlayerViewController()

            if (asset != null) {
                PHImageManager.defaultManager().requestAVAssetForVideo(
                    asset,
                    options = null
                ) { avAsset, _, _ ->
                    avAsset?.let {
                        val playerItem = AVPlayerItem(asset = it)
                        val createdPlayer = AVPlayer(playerItem = playerItem)
                        controller.player = createdPlayer
                        createdPlayer.play()
                        player = createdPlayer
                    }
                }
            }

            controller
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(videoUri) {
        onDispose {
            player?.pause()
            player?.replaceCurrentItemWithPlayerItem(null)
        }
    }
}