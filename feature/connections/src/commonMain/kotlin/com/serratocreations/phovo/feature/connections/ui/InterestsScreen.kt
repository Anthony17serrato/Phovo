package com.serratocreations.phovo.feature.connections.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent

@Composable
internal fun ConnectionsRoute(
    modifier: Modifier = Modifier
) {
    ConnectionsScreen(
        modifier = modifier
    )
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun ConnectionsScreen(
    modifier: Modifier = Modifier,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CallToActionComponent(
            actionTitle = "Configure as server",
            actionDescription = "Configure this device as a Phovo backup server. Your photos and media will be securely backed up to this device.",
            onClick = { /* TODO */ }
        )
    }
}