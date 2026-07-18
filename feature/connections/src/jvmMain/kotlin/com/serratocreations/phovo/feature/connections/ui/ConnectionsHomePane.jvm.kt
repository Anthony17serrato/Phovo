package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent

@Composable
internal actual fun ConnectionsHomePane(
    onConfigClick: () -> Unit,
    connectionsViewModel: ConnectionsViewModel,
    modifier: Modifier
) {
    connectionsViewModel as ServerConnectionsViewModel
    val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
    ConnectionsHomePane(
        uiState = connectionsUiState,
        onConfigClick = onConfigClick,
        modifier = modifier
    )
}

@Composable
internal fun ConnectionsHomePane(
    uiState: ServerConnectionsUiState,
    onConfigClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp)
    ) {
        // Desktop Server UI
        item {
            AnimatedVisibility(uiState.isCurrentDeviceServerConfigured.not()) {
                CallToActionComponent(
                    actionTitle = "Configure as server",
                    actionDescription = "Configure this device as a Phovo backup server. Your photos and media will be securely backed up to this device.",
                    onClick = onConfigClick
                )
            }
        }
        if (uiState.hostUrl != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF4CAF50),
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Server Running",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Server URL: ${uiState.hostUrl}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        items(uiState.serverEventLogs) { eventLog ->
            Text(
                text = eventLog,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}