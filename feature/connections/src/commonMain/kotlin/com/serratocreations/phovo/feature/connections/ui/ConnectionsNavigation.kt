package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.common.ui.PhovoPaneMode
import com.serratocreations.phovo.core.common.ui.PhovoViewModel
import com.serratocreations.phovo.core.designsystem.component.PhovoNavOptions
import com.serratocreations.phovo.core.designsystem.util.getPaneMode
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable object ConnectionsRouteComponent: NavKey

fun NavController.navigateToConnections(navOptions: NavOptions? = null) {
    navigate(route = ConnectionsRouteComponent, navOptions)
}

fun NavGraphBuilder.connectionsDetailsScreen(
    appLevelVmStoreOwner: ViewModelStoreOwner
) {
    composable<ConnectionsRouteComponent> {
        val phovoViewModel: PhovoViewModel = koinViewModel(viewModelStoreOwner = appLevelVmStoreOwner)
        ConnectionsDetailsNavigation(phovoViewModel = phovoViewModel)
    }
}

@Composable
internal fun ConnectionsDetailsNavigation(
    phovoViewModel: PhovoViewModel,
    connectionsViewModel: ConnectionsViewModel = koinViewModel(),
    paneMode: PhovoPaneMode = currentWindowAdaptiveInfo().getPaneMode
) {
    val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
    val appUiState by phovoViewModel.phovoUiState.collectAsStateWithLifecycle()

    LaunchedEffect(appUiState.navigationUpClicked) {
        if (appUiState.navigationUpClicked) {
            val canNavigateBack = connectionsViewModel.onBackClick()
            phovoViewModel.showBackButtonIfRequired(canNavigateBack)
            phovoViewModel.onNavigationUpClickHandled()
            if (connectionsUiState.currentConnectionsPane.previousPane == null) {
                phovoViewModel.showBackButtonIfRequired(false)
            }
            // TODO: BackHandler api is still in development for Compose Multiplatform See:
//          https://youtrack.jetbrains.com/issue/CMP-4419
//          BackHandler(listDetailNavigator.canNavigateBack()) {
//              listDetailNavigator.navigateBack()
//          }
        }
    }

    fun navigate(connectionsNavigation: ConnectionsNavigation) {
        phovoViewModel.showBackButtonIfRequired(true)
        connectionsViewModel.navigateToPane(
            connectionsNavigation.pane,
            *connectionsNavigation.options.toTypedArray()
        )
    }

    when (paneMode) {
        PhovoPaneMode.TwoPane -> {
            ConnectionsTwoPaneContent(
                connectionsViewModel = connectionsViewModel,
                currentPane = connectionsUiState.currentConnectionsPane,
                connectionsUiState = connectionsUiState,
                navigate = ::navigate
            )
        }
        PhovoPaneMode.SinglePane -> {
            ConnectionsSinglePaneContent(
                connectionsViewModel = connectionsViewModel,
                currentPane = connectionsUiState.currentConnectionsPane,
                connectionsUiState = connectionsUiState,
                navigate = ::navigate
            )
        }
    }
//    ConnectionsDetailScreen(
//        connectionsUiState = connectionsUiState,
//        toggleBackVisibility = phovoViewModel::showBackButtonIfRequired,
//        // connectionsViewModel::configureAsServer,
//        paneMode = windowAdaptiveInfo.getPaneMode,
//        phovoViewModel = phovoViewModel
//    )
}
@Composable
fun ConnectionsTwoPaneContent(
    connectionsViewModel: ConnectionsViewModel,
    currentPane: ConnectionsPane,
    connectionsUiState: ConnectionsUiState,
    navigate: (connectionsNavigation: ConnectionsNavigation) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // First pane is permanently home pane
        ConnectionsHomePane(
            onConfigClick = { navigate(ConnectionsNavigation(PaneId.ConfigGettingStarted)) },
            modifier = modifier.weight(1f)
        )
        currentPane.setNavigationContent(
            connectionsViewModel = connectionsViewModel,
            connectionsUiState = connectionsUiState,
            navigate = navigate,
            paneMode = PhovoPaneMode.TwoPane,
            modifier = modifier.weight(1f)
        )
    }
}

@Composable
fun ConnectionsSinglePaneContent(
    connectionsViewModel: ConnectionsViewModel,
    currentPane: ConnectionsPane,
    connectionsUiState: ConnectionsUiState,
    navigate: (connectionsNavigation: ConnectionsNavigation) -> Unit,
) {
    currentPane.setNavigationContent(
        connectionsViewModel = connectionsViewModel,
        connectionsUiState = connectionsUiState,
        navigate = navigate,
        paneMode = PhovoPaneMode.SinglePane
    )
}

@Composable
private fun ConnectionsPane.setNavigationContent(
    connectionsViewModel: ConnectionsViewModel,
    connectionsUiState: ConnectionsUiState,
    navigate: (connectionsNavigation: ConnectionsNavigation) -> Unit,
    paneMode: PhovoPaneMode,
    modifier: Modifier = Modifier
) {
    return when (this) {
        ConnectionsPane.Home -> {
            when (paneMode) {
                PhovoPaneMode.TwoPane -> {
                    ConnectionsDefaultPane(modifier = modifier)
                }
                PhovoPaneMode.SinglePane -> {
                    ConnectionsHomePane(
                        onConfigClick = { navigate(ConnectionsNavigation(PaneId.ConfigGettingStarted)) },
                        modifier = modifier
                        /*highlightSelectedTopic = listDetailNavigator.isDetailPaneVisible(),*/
                    )
                }
            }
        }
        ConnectionsPane.DefaultSecondPane -> ConnectionsDefaultPane(
            modifier = modifier
        )
        is ConnectionsPane.ConfigGettingStarted -> {
            ConfigGettingStartedPane(
                onClickBackup = { navigate(ConnectionsNavigation(PaneId.ConfigStorageSelection)) },
                modifier = modifier
            )
        }
        is ConnectionsPane.ConfigStorageSelection -> {
            ConfigStorageSelectionPane(
                onSelectedDirectory = connectionsViewModel::setSelectedDirectory,
                selectedDirectory = connectionsUiState.selectedDirectory,
                onClickEnableServer = {
                    connectionsViewModel.configureAsServer()
                    navigate(ConnectionsNavigation(PaneId.Home, setOf(PhovoNavOptions.NavigateToBackstack)))
                },
                modifier = modifier
            )
        }
    }
}

data class ConnectionsNavigation(
    val pane: PaneId,
    val options: Set<PhovoNavOptions> = emptySet()
)