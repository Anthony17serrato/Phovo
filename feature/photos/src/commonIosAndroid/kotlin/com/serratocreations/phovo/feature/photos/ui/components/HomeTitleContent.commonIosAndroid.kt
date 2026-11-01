package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun PhotosHomeTitleContent(onFinishServerSetup: () -> Unit) {
    ExpandableBackupBanner(onFinishServerSetup = onFinishServerSetup)
}