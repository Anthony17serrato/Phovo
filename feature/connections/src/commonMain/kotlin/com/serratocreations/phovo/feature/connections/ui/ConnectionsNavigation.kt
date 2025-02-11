package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
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
import androidx.window.core.layout.WindowWidthSizeClass
import com.serratocreations.phovo.core.common.ui.PhovoPaneMode
import com.serratocreations.phovo.core.common.ui.PhovoUiState
import com.serratocreations.phovo.core.common.ui.PhovoViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable object ConnectionsHomeRoute

fun NavController.navigateToConnections(navOptions: NavOptions? = null) {
    navigate(route = ConnectionsHomeRoute, navOptions)
}

fun NavGraphBuilder.connectionsDetailsScreen(
    appLevelVmStoreOwner: ViewModelStoreOwner
) {
    composable<ConnectionsHomeRoute> {
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
        }
    }

    fun navigate(pane: PaneId) {
        phovoViewModel.showBackButtonIfRequired(true)
        connectionsViewModel.navigateToPane(pane)
    }

    when (paneMode) {
        PhovoPaneMode.TwoPane -> {
            ConnectionsTwoPaneContent(
                currentPane = connectionsUiState.currentConnectionsPane,
                connectionsUiState = connectionsUiState,
                appUiState = appUiState,
                navigate = ::navigate
            )
        }
        PhovoPaneMode.SinglePane -> {
            ConnectionsSinglePaneContent(
                currentPane = connectionsUiState.currentConnectionsPane,
                connectionsUiState = connectionsUiState,
                appUiState = appUiState,
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
    currentPane: ConnectionsPane,
    connectionsUiState: ConnectionsUiState,
    appUiState: PhovoUiState,
    navigate: (pane: PaneId) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // First pane is permanently home pane
        ConnectionsHomePane(
            onConfigClick = { navigate(PaneId.ConfigGettingStarted) },
            modifier = modifier.weight(1f)
            /*highlightSelectedTopic = listDetailNavigator.isDetailPaneVisible(),*/
        )
        currentPane.setNavigationContent(
            navigate = navigate,
            paneMode = PhovoPaneMode.TwoPane,
            modifier = modifier.weight(1f)
        )
    }
}

@Composable
fun ConnectionsSinglePaneContent(
    currentPane: ConnectionsPane,
    connectionsUiState: ConnectionsUiState,
    appUiState: PhovoUiState,
    navigate: (pane: PaneId) -> Unit
) {
    currentPane.setNavigationContent(
        navigate = navigate,
        paneMode = PhovoPaneMode.SinglePane
    )
}

@Composable
private fun ConnectionsPane.setNavigationContent(
    navigate: (pane: PaneId) -> Unit,
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
                        onConfigClick = { navigate(PaneId.ConfigGettingStarted) },
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
                onClickBackup = { navigate(PaneId.ConfigStorageSelection) },
                modifier = modifier
            )
        }
        is ConnectionsPane.ConfigStorageSelection -> {
            ConfigStorageSelectionPane(
                modifier = modifier
            )
        }
    }
}

// TODO: BackHandler api is still in development for Compose Multiplatform See:
//  https://youtrack.jetbrains.com/issue/CMP-4419
//    BackHandler(listDetailNavigator.canNavigateBack()) {
//        listDetailNavigator.navigateBack()
//    }

private val WindowAdaptiveInfo.getPaneMode: PhovoPaneMode
    get() = when (this.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.EXPANDED -> PhovoPaneMode.TwoPane
        else -> PhovoPaneMode.SinglePane
    }