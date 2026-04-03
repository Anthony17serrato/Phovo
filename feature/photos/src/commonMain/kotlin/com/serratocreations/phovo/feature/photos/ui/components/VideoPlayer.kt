package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

@Composable
expect fun VideoPlayer(
    videoPlatformFile: PlatformFile,
    modifier: Modifier = Modifier
)