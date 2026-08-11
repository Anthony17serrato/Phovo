package com.serratocreations.phovo.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.common.ui.EXPANDED_WIDTH
import com.serratocreations.phovo.core.common.ui.MEDIUM_WIDTH

/**
 * Height of the floating navigation bar that [PhovoNavigationSuiteScaffold] overlays on top of its
 * content on compact screens, or `0.dp` when no floating bar is shown (bar hidden, or medium and
 * expanded layouts that use a rail or drawer instead).
 *
 * The floating bar is drawn as an overlay and therefore consumes no layout space, so screens whose
 * content reaches the bottom edge should add this to their bottom padding to stay clear of it. It
 * covers the bar itself only — apply [navigationBarsPadding] separately for the system inset.
 */
val LocalFloatingNavBarHeight = compositionLocalOf { 0.dp }

/**
 * Phovo navigation bar item with icon and label content slots. Wraps Material 3
 * [NavigationBarItem].
 *
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is selected.
 * @param icon The item icon content.
 * @param modifier Modifier to be applied to this item.
 * @param selectedIcon The item icon content when selected.
 * @param enabled controls the enabled state of this item. When `false`, this item will not be
 * clickable and will appear disabled to accessibility services.
 * @param label The item text label content.
 */
@Composable
fun PhovoNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit = icon,
    label: @Composable () -> Unit,
) {
    val backgroundColor = if (selected) PhovoNavigationDefaults.navigationIndicatorColor() else Color.Transparent
    val contentColor = if (selected) PhovoNavigationDefaults.navigationSelectedItemColor() else PhovoNavigationDefaults.navigationContentColor()

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                selectedIcon()
                Spacer(Modifier.width(6.dp))
            }
            label()
        }
    }
}

/**
 * Phovo navigation bar with content slot. Wraps Material 3 [NavigationBar].
 *
 * @param modifier Modifier to be applied to the navigation bar.
 * @param content Destinations inside the navigation bar. This should contain multiple
 * [NavigationBarItem]s.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhovoNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors(),
        // Tighter than FloatingToolbarDefaults.ContentPadding, which is 8.dp on every side.
        contentPadding = PaddingValues(6.dp),
        content = {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = { content() }
            )
        }
    )
}

/**
 * Phovo navigation rail item with icon and label content slots. Wraps Material 3
 * [NavigationRailItem].
 *
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is selected.
 * @param icon The item icon content.
 * @param modifier Modifier to be applied to this item.
 * @param selectedIcon The item icon content when selected.
 * @param enabled controls the enabled state of this item. When `false`, this item will not be
 * clickable and will appear disabled to accessibility services.
 * @param label The item text label content.
 * @param alwaysShowLabel Whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 */
@Composable
fun PhovoNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit = icon,
    label: @Composable () -> Unit,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = PhovoNavigationDefaults.navigationContentColor(),
            selectedTextColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = PhovoNavigationDefaults.navigationContentColor(),
            indicatorColor = PhovoNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

/**
 * Phovo navigation rail with header and content slots. Wraps Material 3 [NavigationRail].
 *
 * @param modifier Modifier to be applied to the navigation rail.
 * @param header Optional header that may hold a floating action button or a logo.
 * @param content Destinations inside the navigation rail. This should contain multiple
 * [NavigationRailItem]s.
 */
@Composable
fun PhovoNavigationRail(
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = PhovoNavigationDefaults.navigationContentColor(),
        header = header,
        content = content,
    )
}

class PhovoNavigationSuiteItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val modifier: Modifier,
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val label: @Composable () -> Unit
)

class PhovoNavigationSuiteScope {
    val items = mutableListOf<PhovoNavigationSuiteItem>()

    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit,
        selectedIcon: @Composable () -> Unit = icon,
        label: @Composable () -> Unit,
    ) {
        items.add(
            PhovoNavigationSuiteItem(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                icon = icon,
                selectedIcon = selectedIcon,
                label = label
            )
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PhovoNavigationSuiteScaffold(
    navigationSuiteItems: PhovoNavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    shouldShowNavBarOnCompactScreens: Boolean,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
    content: @Composable () -> Unit,
) {
    val isExpanded = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(EXPANDED_WIDTH)
    val isMedium = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(MEDIUM_WIDTH)

    val scope = PhovoNavigationSuiteScope()
    scope.run(navigationSuiteItems)

    when {
        isExpanded -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet {
                        Spacer(Modifier.height(12.dp))
                        scope.items.forEach { item ->
                            val drawerItemColors = NavigationDrawerItemDefaults.colors(
                                selectedIconColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
                                unselectedIconColor = PhovoNavigationDefaults.navigationContentColor(),
                                selectedTextColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
                                unselectedTextColor = PhovoNavigationDefaults.navigationContentColor(),
                                selectedContainerColor = PhovoNavigationDefaults.navigationIndicatorColor(),
                            )
                            NavigationDrawerItem(
                                icon = {
                                    if (item.selected) item.selectedIcon() else item.icon()
                                },
                                label = item.label,
                                selected = item.selected,
                                onClick = item.onClick,
                                modifier = item.modifier.padding(horizontal = 12.dp),
                                colors = drawerItemColors
                            )
                        }
                    }
                },
                modifier = modifier
            ) {
                content()
            }
        }
        isMedium -> {
            Row(modifier = modifier.fillMaxSize()) {
                PhovoNavigationRail {
                    scope.items.forEach { item ->
                        PhovoNavigationRailItem(
                            selected = item.selected,
                            onClick = item.onClick,
                            icon = item.icon,
                            selectedIcon = item.selectedIcon,
                            label = item.label,
                            modifier = item.modifier
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content()
                }
            }
        }
        else -> {
            val density = LocalDensity.current
            var navBarHeight by remember { mutableStateOf(0.dp) }

            Box(modifier = modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalFloatingNavBarHeight provides
                        if (shouldShowNavBarOnCompactScreens) navBarHeight else 0.dp
                ) {
                    content()
                }

                AnimatedVisibility(
                    visible = shouldShowNavBarOnCompactScreens,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                ) {
                    PhovoNavigationBar(
                        modifier = Modifier.onSizeChanged { size ->
                            navBarHeight = with(density) { size.height.toDp() }
                        }
                    ) {
                        scope.items.forEach { item ->
                            PhovoNavigationBarItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                icon = item.icon,
                                selectedIcon = item.selectedIcon,
                                label = item.label,
                                modifier = item.modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Phovo navigation default values.
 */
object PhovoNavigationDefaults {
    @Composable
    fun navigationContentColor() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun navigationSelectedItemColor() = MaterialTheme.colorScheme.onPrimaryContainer

    @Composable
    fun navigationIndicatorColor() = MaterialTheme.colorScheme.primaryContainer
}