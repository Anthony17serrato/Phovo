package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import com.serratocreations.phovo.core.common.util.toPhAsset
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVKit.AVPlayerViewController
import platform.Photos.PHImageManager

@Composable
actual fun VideoPlayer(
    videoPlatformFile: PlatformFile,
    modifier: Modifier
) {
    var player: AVPlayer? by remember { mutableStateOf(null) }

    UIKitViewController(
        factory = {
            val asset = videoPlatformFile.toPhAsset()

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

    DisposableEffect("${videoPlatformFile.hashCode()}-${videoPlatformFile.extension}") {
        onDispose {
            player?.pause()
            player?.replaceCurrentItemWithPlayerItem(null)
        }
    }
}