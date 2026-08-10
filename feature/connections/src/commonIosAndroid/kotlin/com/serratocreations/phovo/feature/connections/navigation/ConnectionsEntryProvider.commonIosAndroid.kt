package com.serratocreations.phovo.feature.connections.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.feature.connections.ui.ClientConnectionsViewModel
import com.serratocreations.phovo.feature.connections.ui.ConnectionsDefaultPane
import com.serratocreations.phovo.feature.connections.ui.ConnectionsHomePane
import com.serratocreations.phovo.feature.connections.ui.OnboardingPermissionPrimerScreen
import com.serratocreations.phovo.feature.connections.ui.OnboardingServerDiscoveryScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import phovo.feature.connections.generated.resources.Res
import phovo.feature.connections.generated.resources.feature_connections_title

import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
actual fun EntryProviderScope<NavKey>.flavorConnectionsEntries(
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
) {
    entry<ConnectionsHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() },
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = { ConnectionsDefaultPane() }
        )
    ) {
        val connectionsViewModel: ClientConnectionsViewModel = koinViewModel()
        val uiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
        val appBarConfig = remember {
            AppBarConfig(
                title = { Text(stringResource(Res.string.feature_connections_title)) },
                topAppBarColors = {
                    val defaultColors = TopAppBarDefaults.topAppBarColors()
                    defaultColors.copy(
                        containerColor = defaultColors.containerColor.copy(alpha = 0f),
                        scrolledContainerColor = defaultColors.containerColor.copy(alpha = 0f)
                    )
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == ConnectionsHomeNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        ConnectionsHomePane(
            uiState = uiState,
            onDisconnectFromServer = connectionsViewModel::disconnectFromServer,
            onNextStep = {
                if (uiState.localNetworkPermissionStatus == PermissionStatus.Granted) {
                    navigationViewModel.navigate(ClientOnboardingServerDiscoveryNavKey)
                } else {
                    navigationViewModel.navigate(ClientOnboardingPermissionPrimerNavKey)
                }
            },
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }

    entry<ClientOnboardingPermissionPrimerNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = ConnectionsHomeNavKey.toContentKey()
        )
    ) {
        val connectionsViewModel: ClientConnectionsViewModel = koinViewModel()
        val appBarConfig = remember {
            AppBarConfig(
                title = { Text("Local Discovery") },
                navigationIcon = {
                    DefaultNavigationIcon(navigationViewModel::goBack)
                },
                topAppBarColors = {
                    val defaultColors = TopAppBarDefaults.topAppBarColors()
                    defaultColors.copy(
                        containerColor = defaultColors.containerColor.copy(alpha = 0f),
                        scrolledContainerColor = defaultColors.containerColor.copy(alpha = 0f)
                    )
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == ClientOnboardingPermissionPrimerNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        OnboardingPermissionPrimerScreen(
            onRequestPermission = {
                connectionsViewModel.requestLocalNetworkPermission()
                navigationViewModel.navigate(ClientOnboardingServerDiscoveryNavKey)
            },
            onSkipToManual = {
                navigationViewModel.navigate(ClientOnboardingServerDiscoveryNavKey)
            },
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }

    entry<ClientOnboardingServerDiscoveryNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = ConnectionsHomeNavKey.toContentKey()
        )
    ) {
        val connectionsViewModel: ClientConnectionsViewModel = koinViewModel()
        val uiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
        val appBarConfig = remember {
            AppBarConfig(
                title = { Text("Connect to Server") },
                navigationIcon = {
                    DefaultNavigationIcon(navigationViewModel::goBack)
                },
                topAppBarColors = {
                    val defaultColors = TopAppBarDefaults.topAppBarColors()
                    defaultColors.copy(
                        containerColor = defaultColors.containerColor.copy(alpha = 0f),
                        scrolledContainerColor = defaultColors.containerColor.copy(alpha = 0f)
                    )
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == ClientOnboardingServerDiscoveryNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        OnboardingServerDiscoveryScreen(
            uiState = uiState,
            onConnectToServer = { server ->
                connectionsViewModel.connectToServer(server)
                navigationViewModel.popTo(ConnectionsHomeNavKey)
            },
            onConnectManually = { url ->
                connectionsViewModel.connectManually(url)
                navigationViewModel.popTo(ConnectionsHomeNavKey)
            },
            onStartScan = connectionsViewModel::startDiscovery,
            onToggleManualUrlExpanded = connectionsViewModel::toggleManualUrlExpanded,
            onRequestPermission = connectionsViewModel::requestLocalNetworkPermission,
            onOpenSettings = connectionsViewModel::openPermissionSettings,
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
}
