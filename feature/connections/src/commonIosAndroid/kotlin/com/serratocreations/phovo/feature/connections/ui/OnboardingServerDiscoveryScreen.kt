package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.server.data.model.DiscoveredServer
import org.jetbrains.compose.resources.painterResource
import phovo.feature.connections.generated.resources.*

@Composable
internal fun OnboardingServerDiscoveryScreen(
    uiState: ClientConnectionsUiState,
    onConnectToServer: (DiscoveredServer) -> Unit,
    onConnectManually: (String) -> Unit,
    onStartScan: () -> Unit,
    onToggleManualUrlExpanded: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manualUrl by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Keep the manual URL field reachable once the keyboard is up.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        DesktopRequirementBanner(modifier = Modifier.padding(bottom = 16.dp))

        if (uiState.localNetworkPermissionStatus != PermissionStatus.Granted) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local Network Access Off",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.localNetworkPermissionStatus == PermissionStatus.PermanentlyDenied) {
                            "Local network permission is permanently denied. You can connect manually below or enable permission in system settings."
                        } else {
                            "Automatic discovery requires local network permission. Grant permission below or connect manually."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.localNetworkPermissionStatus == PermissionStatus.PermanentlyDenied) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Enable in Settings")
                        }
                    } else {
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Grant Permission")
                        }
                    }
                }
            }

            ManualConnectionCard(
                manualUrl = manualUrl,
                onUrlChange = { manualUrl = it },
                isPairing = uiState.isPairing,
                pairingError = uiState.manualPairingError,
                onConnectManually = onConnectManually
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Servers",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                if (uiState.isSearching) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Searching",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    TextButton(
                        onClick = onStartScan,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_refresh_default),
                            contentDescription = "Rescan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Scan")
                    }
                }
            }

            if (uiState.discoveredServers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_dns_default),
                            contentDescription = "No Servers",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No servers found yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Download & install Phovo Desktop from phovo.app\n2. Open Phovo Desktop on your Mac or PC\n3. Ensure both devices are on the same Wi-Fi network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            } else {
                uiState.discoveredServers.forEach { server ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = server.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = server.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onConnectToServer(server) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Connect")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onToggleManualUrlExpanded) {
                    Text(
                        text = if (uiState.isManualUrlExpanded) "Hide manual server URL entry"
                        else "Trouble connecting? Connect manually",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isManualUrlExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    ManualConnectionCard(
                        manualUrl = manualUrl,
                        onUrlChange = { manualUrl = it },
                        isPairing = uiState.isPairing,
                        pairingError = uiState.manualPairingError,
                        onConnectManually = onConnectManually
                    )
                }
            }
        }
    }
}

