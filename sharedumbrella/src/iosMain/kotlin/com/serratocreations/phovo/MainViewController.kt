package com.serratocreations.phovo

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.serratocreations.phovo.data.permissions.IosPermissionRepository
import com.serratocreations.phovo.ui.PhovoApp
import org.koin.compose.koinInject

// Used in IOS
@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    val permissionRepository: IosPermissionRepository = koinInject()
    val permissionsState by permissionRepository.permissionsState.collectAsState()

    // Basically this logic is the splash screen condition for IOS
    if (permissionsState != null) {
        PhovoApp()
    }
    // Empty composable keeps launch screen appearance until permissions are initialized
}