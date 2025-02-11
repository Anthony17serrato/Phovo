package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ConfigStorageSelectionPane(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Setup storage location",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Select a storage directory on this device to store backups of photos and videos from all of your devices.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}