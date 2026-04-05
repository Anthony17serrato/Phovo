package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

@Composable
expect fun ConfigStorageSelectionPane(
    onSelectedDirectory: (PlatformFile) -> Unit,
    selectedDirectory: PlatformFile?,
    onClickEnableServer: () -> Unit,
    modifier: Modifier = Modifier
)