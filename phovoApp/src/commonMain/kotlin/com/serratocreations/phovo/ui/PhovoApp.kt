package com.serratocreations.phovo.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.serratocreations.phovo.core.common.ui.PhovoViewModel
import com.serratocreations.phovo.core.designsystem.component.PhovoBackground
import com.serratocreations.phovo.core.designsystem.component.PhovoNavigationSuiteScaffold
import com.serratocreations.phovo.ui.components.PhovoTopAppBar
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.core.designsystem.model.ImageVectorIcon
import com.serratocreations.phovo.core.designsystem.model.PainterVectorIcon
import com.serratocreations.phovo.core.designsystem.theme.PhovoTheme
import com.serratocreations.phovo.core.navigation.NavigationState
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.feature.photos.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.core.navigation.rememberNavigationState
import com.serratocreations.phovo.feature.connections.ui.ConnectionsRouteComponent
import com.serratocreations.phovo.feature.connections.ui.connectionsEntries
import com.serratocreations.phovo.feature.photos.navigation.photosEntries
import com.serratocreations.phovo.navigation.PhovoNavSavedStateConfiguration
import com.serratocreations.phovo.navigation.TOP_LEVEL_NAV_ITEMS
import com.serratocreations.phovo.navigation.flavorEntries
import com.serratocreations.phovo.navigation.searchEntries
import com.serratocreations.phovo.ui.viewmodel.ApplicationViewModel
import com.serratocreations.phovo.ui.viewmodel.ServerStatusColor
import org.jetbrains.compose.resources.painterResource
import phovo.phovoapp.generated.resources.Res
import phovo.phovoapp.generated.resources.feature_settings_top_app_bar_action_icon_description
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
@Preview
fun PhovoApp(
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    PhovoTheme {
        PhovoBackground(modifier = modifier) {
            InternalPhovoApp(
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
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    navigationState: NavigationState = rememberNavigationState(
        startRoute = PhotosHomeNavKey,
        topLevelRoutes = TOP_LEVEL_NAV_ITEMS.keys,
        savedStateConfig = PhovoNavSavedStateConfiguration
    ),
    navigationViewModel: NavigationViewModel = koinViewModel(parameters = { parametersOf(navigationState) }),
    // TODO: Merge PhovoViewModel into NavigationViewmodel
    phovoViewModel: PhovoViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner),
    applicationViewModel: ApplicationViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner),
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val applicationUiSate by applicationViewModel.applicationUiState.collectAsState()
    val appBarState by navigationViewModel.appBarState.collectAsState()

    PhovoNavigationSuiteScaffold(
        navigationSuiteItems = {
            TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                val selected = navKey == navigationState.topLevelRoute
                val customModifier = if (navKey == ConnectionsRouteComponent) {
                    Modifier.notificationDot(applicationUiSate.serverStatusColor)
                } else { Modifier }
                item(
                    selected = selected,
                    onClick = { navigationViewModel.navigate(navKey) },
                    icon = {
                        when(navItem.unselectedIcon) {
                            is ImageVectorIcon -> {
                                Icon(
                                    imageVector = navItem.unselectedIcon.icon,
                                    contentDescription = null,
                                )
                            }
                            is PainterVectorIcon -> {
                                Icon(
                                    painter = painterResource( navItem.unselectedIcon.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    selectedIcon = {
                        when(navItem.selectedIcon) {
                            is ImageVectorIcon -> {
                                Icon(
                                    imageVector = navItem.selectedIcon.icon,
                                    contentDescription = null,
                                )
                            }
                            is PainterVectorIcon -> {
                                Icon(
                                    painter = painterResource( navItem.selectedIcon.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    label = { Text(stringResource(navItem.iconTextId)) },
                    modifier = Modifier.testTag("PhovoNavItem")
                        .then(customModifier)
                )
            }
        },
        shouldShowNavBarOnCompactScreens = navigationState.currentKey.isTopLevel(),
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            topBar = {
                PhovoTopAppBar(
                    appBarState = appBarState,
                    actionIcon = PhovoIcons.More,
                    actionIconContentDescription = stringResource(
                        Res.string.feature_settings_top_app_bar_action_icon_description,
                    ),
                    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    menuOptions = applicationUiSate.menuOptions,
                    onMenuActionClick = { navigationViewModel.navigate(route = it) },
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    modifier = Modifier.consumeWindowInsets(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    ),
                ) {
                    SharedTransitionLayout {
                        val entryProvider = entryProvider {
                            photosEntries(
                                sharedElementTransition = this@SharedTransitionLayout,
                                navigationViewModel = navigationViewModel
                            )
                            searchEntries(navigationViewModel = navigationViewModel)
                            connectionsEntries(phovoViewModel = phovoViewModel, navigationViewModel = navigationViewModel)
                            flavorEntries(navigationViewModel)
                        }
                        NavDisplay(
                            entries = navigationState.toDecoratedEntries(entryProvider),
                            //sceneStrategy = listDetailStrategy
                            onBack = {
                                navigationViewModel.goBack()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun NavKey?.isTopLevel() =
    TOP_LEVEL_NAV_ITEMS.keys.any { key ->
        key == this@isTopLevel
    }

private fun Modifier.notificationDot(statusColor: ServerStatusColor): Modifier =
    composed {
        val color = when(statusColor) {
            ServerStatusColor.Green -> MaterialTheme.colorScheme.primary
            ServerStatusColor.Red -> MaterialTheme.colorScheme.error
            ServerStatusColor.Unavailable -> MaterialTheme.colorScheme.onSurfaceVariant
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