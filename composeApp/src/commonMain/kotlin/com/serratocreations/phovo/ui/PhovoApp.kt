package com.serratocreations.phovo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.serratocreations.phovo.core.common.ui.PhovoViewModel
import com.serratocreations.phovo.navigation.PhovoNavHost
import com.serratocreations.phovo.core.designsystem.component.PhovoBackground
import com.serratocreations.phovo.core.designsystem.component.PhovoNavigationSuiteScaffold
import com.serratocreations.phovo.core.designsystem.component.PhovoTopAppBar
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.core.designsystem.theme.PhovoTheme
import com.serratocreations.phovo.navigation.TopLevelDestination
import phovo.composeapp.generated.resources.Res
import phovo.composeapp.generated.resources.feature_settings_top_app_bar_action_icon_description
import phovo.composeapp.generated.resources.feature_settings_top_app_bar_navigation_icon_description
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KClass

@Composable
@Preview
fun PhovoApp(
    appState: PhovoAppState = rememberPhovoAppState(),
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    PhovoTheme {
        PhovoBackground(modifier = modifier) {
            val snackbarHostState = remember { SnackbarHostState() }
            PhovoApp(
                appState = appState,
                snackbarHostState = snackbarHostState,
                windowAdaptiveInfo = windowAdaptiveInfo,
            )
        }
    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
internal fun PhovoApp(
    appState: PhovoAppState,
    snackbarHostState: SnackbarHostState,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    phovoViewModel: PhovoViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner),
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val currentDestination = appState.currentDestination
    appState.appLevelVmStoreOwner = viewModelStoreOwner
    val appLevelUiState by phovoViewModel.phovoUiState.collectAsState()

    PhovoNavigationSuiteScaffold(
        navigationSuiteItems = {
            appState.topLevelDestinations.forEach { destination ->
                val selected = currentDestination
                    .isRouteInHierarchy(destination.route)
                item(
                    selected = selected,
                    onClick = { appState.navigateToTopLevelDestination(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = destination.selectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.iconTextId)) },
                    modifier = Modifier.testTag("PhovoNavItem")
                )
            }
        },
        shouldShowNavBarOnCompactScreens = currentDestination.isTopLevel(),
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                // Show the top app bar on top level destinations.
                val destination = appState.currentTopLevelDestination
                var shouldShowTopAppBar = false

                if (destination != null) {
                    shouldShowTopAppBar = true
                    PhovoTopAppBar(
                        titleRes = destination.titleTextId,
                        navigationIcon = if (appLevelUiState.canBackButtonBeShown) PhovoIcons.ArrowBack else PhovoIcons.Search,
                        navigationIconContentDescription = stringResource(
                            Res.string.feature_settings_top_app_bar_navigation_icon_description,
                        ),
                        actionIcon = PhovoIcons.Settings,
                        actionIconContentDescription = stringResource(
                            Res.string.feature_settings_top_app_bar_action_icon_description,
                        ),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        /*onActionClick = { onTopAppBarActionClick() },*/
                        onNavigationClick = phovoViewModel::onNavigationClick//{ appState.navController.popBackStack() },
                    )
                }

                Box(
                    // Workaround for https://issuetracker.google.com/338478720
                    modifier = Modifier.consumeWindowInsets(
                        if (shouldShowTopAppBar) {
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        } else {
                            WindowInsets(0, 0, 0, 0)
                        },
                    ),
                ) {
                    PhovoNavHost(
                        appState = appState,
                    )
                }

                // TODO: We may want to add padding or spacer when the snackbar is shown so that
                //  content doesn't display behind it.
            }
        }
    }
}

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false

private fun NavDestination?.isTopLevel() =
    TopLevelDestination.entries.any { destination ->
        // default to true when destination is unknown
        this?.hasRoute(destination.route) ?: true
    }