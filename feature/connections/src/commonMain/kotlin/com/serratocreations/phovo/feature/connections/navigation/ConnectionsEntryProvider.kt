package com.serratocreations.phovo.feature.connections.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.DefaultNavigationIcon
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.core.navigation.SharedViewModelStoreNavEntryDecorator
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.feature.connections.ui.ConfigGettingStartedPane
import com.serratocreations.phovo.feature.connections.ui.ConfigStorageSelectionPane
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

    entry<ConfigGettingStartedNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = ConnectionsHomeNavKey.toContentKey()
        ) + ListDetailSceneStrategy.detailPane()
    ) {
        val connectionsViewModel: ConnectionsViewModel = koinViewModel()
        val appBarConfig = remember {
            AppBarConfig(
                title = { Text("Server Configuration") },
                navigationIcon = {
                    DefaultNavigationIcon(navigationViewModel::goBack)
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == ConfigGettingStartedNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        ConfigGettingStartedPane(
            onClickBackup = {
                navigationViewModel.navigate(ConfigStorageSelectionNavKey)
            },
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }

    entry<ConfigStorageSelectionNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = ConnectionsHomeNavKey.toContentKey()
        ) + ListDetailSceneStrategy.detailPane()
    ) {
        val connectionsViewModel: ConnectionsViewModel = koinViewModel()
        val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
        val appBarConfig = remember {
            AppBarConfig(
                title = { Text("Select Storage") },
                navigationIcon = {
                    DefaultNavigationIcon(navigationViewModel::goBack)
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == ConfigStorageSelectionNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        ConfigStorageSelectionPane(
            onSelectedDirectory = connectionsViewModel::setSelectedDirectory,
            selectedDirectory = connectionsUiState.selectedDirectory,
            onClickEnableServer = {
                connectionsViewModel.configureAsServer()
                navigationViewModel.popTo(ConnectionsHomeNavKey)
            },
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
}
