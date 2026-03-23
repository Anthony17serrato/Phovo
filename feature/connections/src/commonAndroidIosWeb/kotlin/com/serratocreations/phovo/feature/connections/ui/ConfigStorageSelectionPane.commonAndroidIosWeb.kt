package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

@Composable
actual fun ConfigStorageSelectionPane(
    onSelectedDirectory: (PlatformFile) -> Unit,
    selectedDirectory: PlatformFile?,
    onClickEnableServer: () -> Unit,
    modifier: Modifier
) {
    // TODO: this is a desktop only feature, Ideally the pane would be defined in a separate
    //  desktop only  module
    // NO-OP
}