package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.vinceglb.filekit.PlatformFile

@Composable
actual fun VideoPlayer(videoPlatformFile: PlatformFile, modifier: Modifier) {
    val playerState = rememberVideoPlayerState()
    // TODO: Need to add alternate UI for unsupported video formats that would
    //  allow the user to open the video on an external application using below API:
    // handleVideoDesktop(uriPhotoUiItem.sourceAsset)
    LaunchedEffect(videoPlatformFile) {
        playerState.openFile(videoPlatformFile)
    }
    // Video Surface
    // TODO Library has orientation issues: https://github.com/kdroidFilter/ComposeMediaPlayer/issues/202
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VideoPlayerSurface(
            playerState = playerState,
            //contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}