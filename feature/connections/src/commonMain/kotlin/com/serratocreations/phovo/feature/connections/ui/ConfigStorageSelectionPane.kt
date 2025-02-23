package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher

@Composable
fun ConfigStorageSelectionPane(
    onSelectedDirectory: (String) -> Unit,
    selectedDirectory: String?,
    onClickEnableServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Setup storage location",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Select a storage directory on this device to store backups of photos and videos from all of your devices.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        // FileKit Compose
        val launcher = rememberDirectoryPickerLauncher(
            title = "Select a backup directory"
        ) { directory ->
            directory?.path?.let {
                onSelectedDirectory(it)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = selectedDirectory ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text("Selected directory") },
            modifier = Modifier.clickable {
                launcher.launch()
            }
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { launcher.launch() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select a Directory")
        }
        AnimatedVisibility(visible = selectedDirectory != null) {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                ExtendedFloatingActionButton(
                    onClick = onClickEnableServer,
                    icon = { Icon(PhovoIcons.Check, contentDescription = "checkmark icon") },
                    text = { Text(text = "Enable Server") },
                    modifier = Modifier.align(Alignment.Bottom)
                )
            }
        }
    }
}