package com.serratocreations.phovo.feature.connections.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.feature.connections.ui.ConnectionsDefaultPane
import com.serratocreations.phovo.feature.connections.ui.ConnectionsHomePane
import com.serratocreations.phovo.feature.connections.ui.ConnectionsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import phovo.feature.connections.generated.resources.Res
import phovo.feature.connections.generated.resources.feature_connections_title

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.connectionsEntries(
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
) {
    flavorConnectionsEntries(navigationViewModel, scaffoldPadding)

    entry<ConnectionsHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() },
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = { ConnectionsDefaultPane() }
        )
    ) {
        val connectionsViewModel: ConnectionsViewModel = koinViewModel()
        val appBarConfig = remember {
            AppBarConfig(
                title = { Text(stringResource(Res.string.feature_connections_title)) }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == ConnectionsHomeNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        ConnectionsHomePane(
            onConfigClick = {
                navigationViewModel.navigate(ConfigGettingStartedNavKey)
            },
            connectionsViewModel = connectionsViewModel,
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
}

expect fun EntryProviderScope<NavKey>.flavorConnectionsEntries(
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
)