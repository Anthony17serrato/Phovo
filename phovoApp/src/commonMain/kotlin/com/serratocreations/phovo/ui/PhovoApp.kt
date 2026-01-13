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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.serratocreations.phovo.core.navigation.Navigator
import com.serratocreations.phovo.navigation.TopLevelDestination
import com.serratocreations.phovo.ui.components.HomeTitleContent
import com.serratocreations.phovo.ui.viewmodel.ApplicationViewModel
import com.serratocreations.phovo.ui.viewmodel.Green
import com.serratocreations.phovo.ui.viewmodel.Red
import com.serratocreations.phovo.ui.viewmodel.ServerStatusColor
import com.serratocreations.phovo.ui.viewmodel.Unavailable
import phovo.phovoapp.generated.resources.Res
import phovo.phovoapp.generated.resources.feature_settings_top_app_bar_action_icon_description
import phovo.phovoapp.generated.resources.feature_settings_top_app_bar_navigation_icon_description
import org.jetbrains.compose.resources.stringResource
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
            InternalPhovoApp(
                appState = appState,
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
internal fun InternalPhovoApp(
    appState: PhovoAppState,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    phovoViewModel: PhovoViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner),
    applicationViewModel: ApplicationViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner),
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val currentDestination = appState.currentDestination
    appState.appLevelVmStoreOwner = viewModelStoreOwner
    val appLevelUiState by phovoViewModel.phovoUiState.collectAsState()
    val applicationUiSate by applicationViewModel.applicationUiState.collectAsState()

    val navigator = remember { Navigator(appState.navigationState) }
    PhovoNavigationSuiteScaffold(
        navigationSuiteItems = {
            appState.topLevelDestinations.forEach { destination ->
                val selected = currentDestination
                    .isRouteInHierarchy(destination.route)
                val customModifier = if (destination == TopLevelDestination.Connections) {
                    Modifier.notificationDot(applicationUiSate)
                }
                else { Modifier }
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
                        .then(customModifier)
                )
            }
        },
        shouldShowNavBarOnCompactScreens = currentDestination.isTopLevel(),
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                PhovoTopAppBar(
                    navigationIcon = if (appLevelUiState.canBackButtonBeShown) PhovoIcons.ArrowBack else PhovoIcons.Search,
                    navigationIconContentDescription = stringResource(
                        Res.string.feature_settings_top_app_bar_navigation_icon_description,
                    ),
                    actionIcon = PhovoIcons.More,
                    actionIconContentDescription = stringResource(
                        Res.string.feature_settings_top_app_bar_action_icon_description,
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    /*onActionClick = { onTopAppBarActionClick() },*/
                    onNavigationClick = phovoViewModel::onNavigationClick,//{ appState.navController.popBackStack() }
                    scrollBehavior = scrollBehavior,
                    titleContent = { HomeTitleContent() }
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                Box(
                    // Workaround for https://issuetracker.google.com/338478720
                    modifier = Modifier.consumeWindowInsets(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
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

private fun Modifier.notificationDot(statusColor: ServerStatusColor): Modifier =
    composed {
        val color = when(statusColor) {
            Green -> MaterialTheme.colorScheme.primary
            Red -> MaterialTheme.colorScheme.error
            Unavailable -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        drawWithContent {
            drawContent()
            drawCircle(
                color,
                radius = 5.dp.toPx(),
                // This is based on the dimensions of the NavigationBar's "indicator pill";
                // however, its parameters are private, so we must depend on them implicitly
                // (NavigationBarTokens.ActiveIndicatorWidth = 64.dp)
                center = center + Offset(
                    64.dp.toPx() * .45f,
                    32.dp.toPx() * -.45f - 6.dp.toPx(),
                ),
            )
        }
    }