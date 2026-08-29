package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import phovo.feature.connections.generated.resources.Res
import phovo.feature.connections.generated.resources.ic_link_default

@Composable
internal fun ManualConnectionCard(
    manualUrl: String,
    onUrlChange: (String) -> Unit,
    onConnectManually: (String) -> Unit,
    isPairing: Boolean,
    pairingError: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Manual Server Address",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. Download & install Phovo Desktop from phovo.app\n2. Open Phovo Desktop on your Mac or PC\n3. Enter the server address displayed in the desktop app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = manualUrl,
                onValueChange = onUrlChange,
                label = { Text("Server URL (http://ip:port)") },
                placeholder = { Text("http://192.168.1.100:8080") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_link_default),
                        contentDescription = "URL Link"
                    )
                }
            )

            if (pairingError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pairingError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (manualUrl.isNotBlank()) {
                        onConnectManually(manualUrl.trim())
                    }
                },
                enabled = manualUrl.isNotBlank() && !isPairing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = if (isPairing) "Checking address…" else "Connect Manually")
            }
        }
    }
}
