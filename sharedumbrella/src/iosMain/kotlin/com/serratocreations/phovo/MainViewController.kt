package com.serratocreations.phovo

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.serratocreations.phovo.data.permissions.IosPermissionRepository
import com.serratocreations.phovo.ui.PhovoApp
import org.koin.compose.koinInject

// Used in IOS
@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController(
    configure = {
        // UIKit's default is to slide the whole Compose scene up so the focused field clears the
        // keyboard. Screens here already reserve that space with Modifier.imePadding(), which is
        // the only mechanism Android has, so leaving the slide on compensates twice: the field
        // ends up correctly placed but the top app bar is pushed off-screen.
        onFocusBehavior = OnFocusBehavior.DoNothing
    }
) {
    val permissionRepository: IosPermissionRepository = koinInject()
    val permissionsState by permissionRepository.permissionsState.collectAsState()

    // Basically this logic is the splash screen condition for IOS
    if (permissionsState != null) {
        PhovoApp()
    }
    // Empty composable keeps launch screen appearance until permissions are initialized
}
