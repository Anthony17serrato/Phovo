package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFieldFocused by remember { mutableStateOf(false) }

    // The keyboard covers the bottom of the scroll viewport, and focusing the field only scrolls
    // the field itself into view — which leaves the Connect button underneath the keyboard. Asking
    // for the field-and-button block instead brings both up. The ime inset is a key because it
    // arrives over several frames as the keyboard animates in, and only the settled height puts the
    // button in the right place.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(isFieldFocused, imeBottom) {
        if (isFieldFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    fun submit() {
        if (manualUrl.isNotBlank() && !isPairing) {
            focusManager.clearFocus()
            onConnectManually(manualUrl.trim())
        }
    }

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

            Column(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
                OutlinedTextField(
                    value = manualUrl,
                    onValueChange = onUrlChange,
                    label = { Text("Server URL (http://ip:port)") },
                    placeholder = { Text("http://192.168.1.100:8080") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    // An address is not prose: correcting or capitalising it can only corrupt it.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFieldFocused = it.isFocused },
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
                    onClick = ::submit,
                    enabled = manualUrl.isNotBlank() && !isPairing,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isPairing) "Checking address…" else "Connect Manually")
                }
            }
        }
    }
}
