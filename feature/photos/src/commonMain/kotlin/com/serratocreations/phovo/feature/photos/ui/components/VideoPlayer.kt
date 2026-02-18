package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.Uri

@Composable
expect fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier
)