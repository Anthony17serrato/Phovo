package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun ConnectionsHomePane(
    onConfigClick: () -> Unit,
    connectionsViewModel: ConnectionsViewModel,
    modifier: Modifier
)