package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.designsystem.component.LocalFloatingNavBarHeight

@Composable
internal fun ConnectionsHomePane(
    uiState: ClientConnectionsUiState,
    onDisconnectFromServer: () -> Unit,
    onNextStep: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (uiState.isClientConfigured) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    LocalFloatingNavBarHeight.current + 16.dp
            )
        ) {
            item {
                ClientConnectedPane(
                    serverUrl = uiState.configuredServerUrl ?: "",
                    onDisconnect = onDisconnectFromServer
                )
            }
        }
    } else {
        // Manages its own scrolling so it can pin the "Get Started" button above the floating
        // navigation bar, so it must not be nested inside the LazyColumn.
        OnboardingWelcomeScreen(
            onNextStep = onNextStep,
            modifier = modifier
        )
    }
}
