package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ConnectionsHomeScreen(
    onConfigClick: () -> Unit,
    connectionsViewModel: ConnectionsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
    ConnectionsHomeScreen(
        uiState = connectionsUiState,
        onConfigClick = onConfigClick,
        modifier = modifier
    )
}

@Composable
internal fun ConnectionsHomeScreen(
    uiState: ConnectionsUiState,
    onConfigClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (uiState.doesCurrentDeviceSupportServer) {
            item {
                AnimatedVisibility(uiState.isCurrentDeviceServerConfigured.not()) {
                    CallToActionComponent(
                        actionTitle = "Configure as server",
                        actionDescription = "Configure this device as a Phovo backup server. Your photos and media will be securely backed up to this device.",
                        onClick = onConfigClick
                    )
                }
            }
        }
        items(uiState.serverEventLogs) { eventLog ->
            Text(text = eventLog)
        }
    }
}