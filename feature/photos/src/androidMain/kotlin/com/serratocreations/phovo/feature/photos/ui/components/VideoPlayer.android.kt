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
import coil3.Uri
import coil3.toAndroidUri

@Composable
actual fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        val androidUri = videoUri.toAndroidUri()
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