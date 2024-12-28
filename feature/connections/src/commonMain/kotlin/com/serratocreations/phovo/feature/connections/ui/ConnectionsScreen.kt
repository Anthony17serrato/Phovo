package com.serratocreations.phovo.feature.connections.ui

import androidx.annotation.VisibleForTesting
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
    connectionsViewModel: ConnectionsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
    LazyColumn(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (connectionsUiState.doesCurrentDeviceSupportServer) {
            item {
                AnimatedVisibility(connectionsUiState.isCurrentDeviceServerConfigured.not()) {
                    CallToActionComponent(
                        actionTitle = "Configure as server",
                        actionDescription = "Configure this device as a Phovo backup server. Your photos and media will be securely backed up to this device.",
                        onClick = connectionsViewModel::configureAsServer
                    )
                }
            }
        }
        items(connectionsUiState.serverEventLogs) { eventLog ->
            Text(text = eventLog)
        }
    }
}