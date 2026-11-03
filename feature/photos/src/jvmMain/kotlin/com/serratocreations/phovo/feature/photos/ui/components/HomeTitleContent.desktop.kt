package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.serratocreations.phovo.core.navigation.ExpandableHeaderState

@Composable
actual fun PhotosHomeTitleContent(headerState: ExpandableHeaderState) {
    Text("Phovo")
}

@Composable
actual fun PhotosHomeHeaderContent(onFinishServerSetup: () -> Unit) {}
