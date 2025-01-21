package com.serratocreations.phovo.feature.connections.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.serratocreations.phovo.feature.connections.ui.ConfigGettingStartedScreen
import com.serratocreations.phovo.feature.connections.ui.ConnectionsHomeRoute
import com.serratocreations.phovo.feature.connections.ui.ConnectionsUiState
import com.serratocreations.phovo.feature.connections.ui.ConnectionsViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// TODO: Remove @Keep when https://issuetracker.google.com/353898971 is fixed
@Serializable internal object DetailPaneNavHostRoute

@Serializable object ConnectionsHomeRoute
@Serializable object ConfigGettingStartedRoute

fun NavController.navigateToConfigGettingStarted(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = ConfigGettingStartedRoute) {
        navOptions()
    }
}

fun NavController.navigateToConnections(navOptions: NavOptions? = null) {
    navigate(route = ConnectionsHomeRoute, navOptions)
}

fun NavGraphBuilder.connectionsDetailsScreen() {
    composable<ConnectionsHomeRoute> {
        ConnectionsDetailScreen()
    }
//    // Nested nav graph
//    navigation<ConnectionsGraph>(startDestination = ConnectionsHome) {
//
//        composable<ConfigGettingStarted> { backStackEntry ->
//            val parentEntry = remember(backStackEntry) {
//                navController.getBackStackEntry<ConnectionsGraph>()
//            }
//            val connectionsViewModel: ConnectionsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
//            ConfigGettingStartedScreen(
//                connectionsViewModel = connectionsViewModel,
//            )
//        }
//    }
}

fun NavGraphBuilder.serverConfigScreens(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    navController: NavController
) {
    composable<ConfigGettingStartedRoute> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry<DetailPaneNavHostRoute>()
        }
        val connectionsViewModel: ConnectionsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        ConfigGettingStartedScreen(
            connectionsViewModel = connectionsViewModel,
            showBackButton = showBackButton,
            onBackClick = onBackClick
        )
    }
}

@Composable
internal fun ConnectionsDetailScreen(
    connectionsViewModel: ConnectionsViewModel = koinViewModel(),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val connectionsUiState by connectionsViewModel.connectionsUiState.collectAsStateWithLifecycle()
    ConnectionsDetailScreen(
        connectionsUiState = connectionsUiState,
        onConfigClick = {/* TODO */},
        // connectionsViewModel::configureAsServer,
        windowAdaptiveInfo = windowAdaptiveInfo
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalUuidApi::class)
@Composable
internal fun ConnectionsDetailScreen(
    connectionsUiState: ConnectionsUiState,
    onConfigClick: () -> Unit,
    windowAdaptiveInfo: WindowAdaptiveInfo,
) {
    val listDetailNavigator = rememberListDetailPaneScaffoldNavigator(
        scaffoldDirective = calculatePaneScaffoldDirective(windowAdaptiveInfo),
        initialDestinationHistory = listOfNotNull(
            ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List),
            ThreePaneScaffoldDestinationItem<Nothing>(ListDetailPaneScaffoldRole.Detail).takeIf {
                // TODO: check  state or have some default Pane to show if the user has not selected anything
                true
            },
        ),
    )
    // TODO: BackHandler api is still in development for Compose Multiplatform See:
    //  https://youtrack.jetbrains.com/issue/CMP-4419
//    BackHandler(listDetailNavigator.canNavigateBack()) {
//        listDetailNavigator.navigateBack()
//    }

    var nestedNavHostStartRoute by remember {
        val route =
            ConfigGettingStartedRoute//selectedTopicId?.let { TopicRoute(id = it) } ?: TopicPlaceholderRoute
        mutableStateOf(route)
    }
    var nestedNavKey by rememberSaveable(
        stateSaver = Saver({ it.toString() }, Uuid::parse),
    ) {
        mutableStateOf(Uuid.random())
    }
    val nestedNavController = key(nestedNavKey) {
        rememberNavController()
    }

    fun onConfigServerClickShowDetailPane() {
        onConfigClick()
        if (listDetailNavigator.isDetailPaneVisible()) {
            // If the detail pane was visible, then use the nestedNavController navigate call
            // directly
            nestedNavController.navigateToConfigGettingStarted {
                popUpTo<DetailPaneNavHostRoute>()
            }
        } else {
            // Otherwise, recreate the NavHost entirely, and start at the new destination
            nestedNavHostStartRoute = ConfigGettingStartedRoute
            nestedNavKey = Uuid.random()
        }
        listDetailNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
    }

    ListDetailPaneScaffold(
        value = listDetailNavigator.scaffoldValue,
        directive = listDetailNavigator.scaffoldDirective,
        listPane = {
            AnimatedPane {
                ConnectionsHomeRoute(
                    onConfigClick = ::onConfigServerClickShowDetailPane,
                    /*highlightSelectedTopic = listDetailNavigator.isDetailPaneVisible(),*/
                )
            }
        },
        detailPane = {
            AnimatedPane {
                key(nestedNavKey) {
                    NavHost(
                        navController = nestedNavController,
                        startDestination = nestedNavHostStartRoute,
                        route = DetailPaneNavHostRoute::class,
                    ) {
                        serverConfigScreens(
                            showBackButton = !listDetailNavigator.isListPaneVisible(),
                            onBackClick = listDetailNavigator::navigateBack,
                            navController = nestedNavController
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun <T> ThreePaneScaffoldNavigator<T>.isListPaneVisible(): Boolean =
    scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun <T> ThreePaneScaffoldNavigator<T>.isDetailPaneVisible(): Boolean =
    scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded