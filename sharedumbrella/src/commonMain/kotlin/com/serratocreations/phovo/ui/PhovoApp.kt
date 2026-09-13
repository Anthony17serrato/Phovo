package com.serratocreations.phovo.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
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
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.serratocreations.phovo.core.designsystem.component.PhovoBackground
import com.serratocreations.phovo.core.designsystem.component.PhovoNavigationSuiteScaffold
import com.serratocreations.phovo.core.designsystem.constants.CommonDimensions.defaultIconSize
import com.serratocreations.phovo.ui.components.PhovoTopAppBar
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.core.designsystem.model.ImageVectorIcon
import com.serratocreations.phovo.core.designsystem.model.PainterVectorIcon
import com.serratocreations.phovo.core.designsystem.theme.PhovoTheme
import com.serratocreations.phovo.core.navigation.NavigationState
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.feature.photos.navigation.PhotoDetailNavKey
import com.serratocreations.phovo.feature.photos.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.core.navigation.rememberNavigationState
import com.serratocreations.phovo.feature.connections.navigation.ConnectionsHomeNavKey
import com.serratocreations.phovo.feature.connections.navigation.connectionsEntries
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import com.serratocreations.phovo.feature.photos.extensions.showAppBar
import com.serratocreations.phovo.feature.photos.navigation.photosEntries
import com.serratocreations.phovo.navigation.PhovoNavSavedStateConfiguration
import com.serratocreations.phovo.navigation.TOP_LEVEL_NAV_ITEMS
import com.serratocreations.phovo.navigation.flavorEntries
import com.serratocreations.phovo.navigation.searchEntries
import com.serratocreations.phovo.ui.components.PhovoBottomToolBar
import com.serratocreations.phovo.ui.viewmodel.ApplicationViewModel
import com.serratocreations.phovo.ui.viewmodel.ServerStatusColor
import org.jetbrains.compose.resources.painterResource
import phovo.sharedumbrella.generated.resources.Res
import phovo.sharedumbrella.generated.resources.feature_settings_top_app_bar_action_icon_description
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
@Preview
fun PhovoApp(
    modifier: Modifier = Modifier
) {
    PhovoTheme {
        PhovoBackground(modifier = modifier) {
            InternalPhovoApp()
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
    applicationViewModel: ApplicationViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner),
    modifier: Modifier = Modifier
) {
    val applicationUiSate by applicationViewModel.applicationUiState.collectAsState()
    val appBarState by navigationViewModel.appBarState.collectAsState()

    PhovoNavigationSuiteScaffold(
        navigationSuiteItems = {
            TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                val selected = navKey == navigationState.topLevelRoute
                val customModifier = if (navKey == ConnectionsHomeNavKey) {
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
                                    modifier = Modifier.size(defaultIconSize)
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
                                    modifier = Modifier.size(defaultIconSize)
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
        shouldShowNavBarOnCompactScreens = navigationState.currentKey.isTopLevel()
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
                    menuOptions = applicationUiSate.menuOptions,
                    onMenuActionClick = { navigationViewModel.navigate(route = it) },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                PhovoBottomToolBar(
                    appBarConfig = appBarState
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
                                navigationViewModel = navigationViewModel,
                                onShowAppBarRequested = { scrollBehavior.showAppBar() },
                                scaffoldPadding = padding
                            )
                            searchEntries(
                                navigationViewModel = navigationViewModel,
                                scaffoldPadding = padding
                            )
                            connectionsEntries(
                                navigationViewModel = navigationViewModel,
                                scaffoldPadding = padding
                            )
                            flavorEntries(
                                navigationViewModel = navigationViewModel,
                                scaffoldPadding = padding
                            )
                        }
                        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
                        // Two kinds of navigation cross fade instead of using the platform push:
                        // a lateral move between top level destinations, and a move where a shared
                        // element already carries the motion. Everything else keeps the platform
                        // default.
                        val tabFade = remember { crossFade(TAB_SWITCH_FADE_MILLIS) }
                        val sharedElementFade = remember { crossFade(SHARED_ELEMENT_FADE_MILLIS) }
                        val pushSpec = remember { defaultTransitionSpec<NavKey>() }
                        val popSpec = remember { defaultPopTransitionSpec<NavKey>() }
                        val predictivePopSpec = remember { defaultPredictivePopTransitionSpec<NavKey>() }
                        NavDisplay(
                            entries = navigationState.toDecoratedEntries(entryProvider),
                            sceneStrategies = listOf(listDetailStrategy),
                            transitionSpec = {
                                when {
                                    isSharedElementMove() -> sharedElementFade
                                    isTopLevelSwitch() -> tabFade
                                    else -> pushSpec(this)
                                }
                            },
                            popTransitionSpec = {
                                when {
                                    isSharedElementMove() -> sharedElementFade
                                    isTopLevelSwitch() -> tabFade
                                    else -> popSpec(this)
                                }
                            },
                            predictivePopTransitionSpec = { edge ->
                                when {
                                    isSharedElementMove() -> sharedElementFade
                                    isTopLevelSwitch() -> tabFade
                                    else -> predictivePopSpec(this, edge)
                                }
                            },
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

private const val TAB_SWITCH_FADE_MILLIS = 200

/**
 * Longer than [TAB_SWITCH_FADE_MILLIS] so that the outgoing screen is still on its way out while
 * the shared element travels, rather than vanishing out from under it.
 */
private const val SHARED_ELEMENT_FADE_MILLIS = 300

private fun crossFade(durationMillis: Int) =
    ContentTransform(fadeIn(tween(durationMillis)), fadeOut(tween(durationMillis)))

/**
 * True when both the outgoing and incoming destinations are top level routes, i.e. the user tapped
 * a different item in the navigation bar.
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTopLevelSwitch(): Boolean =
    initialState.entries.last().contentKey in TOP_LEVEL_CONTENT_KEYS &&
        targetState.entries.last().contentKey in TOP_LEVEL_CONTENT_KEYS

/**
 * True when either side of the transition is a destination that draws a shared element. The shared
 * element is what should read as the movement, so sliding the container as well fights it.
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.isSharedElementMove(): Boolean =
    initialState.entries.last().contentKey in SHARED_ELEMENT_CONTENT_KEYS ||
        targetState.entries.last().contentKey in SHARED_ELEMENT_CONTENT_KEYS

private val TOP_LEVEL_CONTENT_KEYS: Set<Any> =
    TOP_LEVEL_NAV_ITEMS.keys.mapTo(mutableSetOf()) { it.toContentKey() }

private val SHARED_ELEMENT_CONTENT_KEYS: Set<Any> =
    setOf(PhotoDetailNavKey.toContentKey())

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
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(
                    x = size.width - 12.dp.toPx(),
                    y = 10.dp.toPx(),
                ),
            )
        }
    }