package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.serratocreations.phovo.core.common.extension.androidUri
import io.github.vinceglb.filekit.PlatformFile

@Composable
actual fun VideoPlayer(
    videoPlatformFile: PlatformFile,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        val androidUri = videoPlatformFile.androidFile.androidUri
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(androidUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
            }
        },
        modifier = modifier.fillMaxSize()
    )
}