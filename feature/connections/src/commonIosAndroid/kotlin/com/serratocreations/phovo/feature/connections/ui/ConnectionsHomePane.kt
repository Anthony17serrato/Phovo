package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal actual fun ConnectionsHomePane(
    onConfigClick: () -> Unit,
    connectionsViewModel: ConnectionsViewModel,
    modifier: Modifier
) {
    connectionsViewModel as ClientConnectionsViewModel
    val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()

    ConnectionsHomePane(
        uiState = connectionsUiState,
        onDisconnectFromServer = connectionsViewModel::disconnectFromServer,
        onNextStep = onConfigClick,
        modifier = modifier
    )
}

@Composable
internal fun ConnectionsHomePane(
    uiState: ClientConnectionsUiState,
    onDisconnectFromServer: () -> Unit,
    onNextStep: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(0.dp)
    ) {
        if (uiState.isClientConfigured) {
            item {
                ClientConnectedPane(
                    serverUrl = uiState.configuredServerUrl ?: "",
                    onDisconnect = onDisconnectFromServer
                )
            }
        } else {
            item {
                OnboardingWelcomeScreen(
                    onNextStep = onNextStep
                )
            }
        }
    }
}