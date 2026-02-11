package com.serratocreations.phovo.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.DevMenuHomeScreen
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.viewmodel.DevViewModel
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.flavorEntries(
    navigationViewmodel: NavigationViewModel
) {
    entry<DevMenuHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() }
    ) {
        val devViewModel: DevViewModel = koinViewModel()
        val uiState by devViewModel.uiState.collectAsStateWithLifecycle()
        DevMenuHomeScreen(
            devMenuItems = uiState.devMenuListOfRoutes,
            onClickMenuItem = { key -> navigationViewmodel.navigate(key) }
        )
    }
    entry<DevMenuResetOptionsNavKey> {
        Text("DevMenuResetOptionsNavKey")
    }
}