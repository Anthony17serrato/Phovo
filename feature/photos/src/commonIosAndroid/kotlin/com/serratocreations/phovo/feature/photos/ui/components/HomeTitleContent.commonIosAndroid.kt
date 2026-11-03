package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable
import com.serratocreations.phovo.core.navigation.ExpandableHeaderState

@Composable
actual fun PhotosHomeTitleContent(headerState: ExpandableHeaderState) {
    BackupStatusChip(headerState = headerState)
}

@Composable
actual fun PhotosHomeHeaderContent(onFinishServerSetup: () -> Unit) {
    BackupSummaryHeader(onFinishServerSetup = onFinishServerSetup)
}
