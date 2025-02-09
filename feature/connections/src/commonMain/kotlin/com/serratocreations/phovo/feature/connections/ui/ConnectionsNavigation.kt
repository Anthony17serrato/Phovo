package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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


//ConfigGettingStartedScreen(
//    connectionsViewModel = connectionsViewModel,
//    showBackButton = showBackButton,
//    onBackClick = onBackClick
//)

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
        }
    }

    fun onConfigServerClickShowDetailPane() {
        phovoViewModel.showBackButtonIfRequired(true)
        connectionsViewModel.showConfigPane()
        // Navigate
    }

    when (paneMode) {
        // TODO Create two pane content
        PhovoPaneMode.TwoPane, PhovoPaneMode.SinglePane -> {
            ConnectionsSinglePaneContent(
                currentPane = connectionsUiState.currentConnectionsPane,
                connectionsUiState = connectionsUiState,
                appUiState = appUiState,
                onConfigServerClickShowDetailPane = ::onConfigServerClickShowDetailPane
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
fun ConnectionsSinglePaneContent(
    currentPane: ConnectionsPane,
    connectionsUiState: ConnectionsUiState,
    appUiState: PhovoUiState,
    onConfigServerClickShowDetailPane: () -> Unit
) {
    when (currentPane) {
        ConnectionsPane.Home -> {
            ConnectionsHomeScreen(
                onConfigClick = onConfigServerClickShowDetailPane,
                /*highlightSelectedTopic = listDetailNavigator.isDetailPaneVisible(),*/
            )
        }
        ConnectionsPane.DefaultSecondPane -> TODO()
        is ConnectionsPane.ConfigGettingStarted -> {
            ConfigGettingStartedScreen(
                showBackButton = true
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