package com.serratocreations.phovo.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.core.designsystem.theme.PhovoTheme

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
 * @param alwaysShowLabel Whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 */
@Composable
fun RowScope.PhovoNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit = icon,
    label: @Composable (() -> Unit)? = null,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = PhovoNavigationDefaults.navigationContentColor(),
            selectedTextColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = PhovoNavigationDefaults.navigationContentColor(),
            indicatorColor = PhovoNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

/**
 * Phovo navigation bar with content slot. Wraps Material 3 [NavigationBar].
 *
 * @param modifier Modifier to be applied to the navigation bar.
 * @param content Destinations inside the navigation bar. This should contain multiple
 * [NavigationBarItem]s.
 */
@Composable
fun PhovoNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        contentColor = PhovoNavigationDefaults.navigationContentColor(),
        tonalElevation = 0.dp,
        content = content,
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
    label: @Composable (() -> Unit)? = null,
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

/**
 * Phovo navigation suite scaffold with item and content slots.
 * Wraps Material 3 [NavigationSuiteScaffold].
 *
 * @param modifier Modifier to be applied to the navigation suite scaffold.
 * @param navigationSuiteItems A slot to display multiple items via [PhovoNavigationSuiteScope].
 * @param windowAdaptiveInfo The window adaptive info.
 * @param content The app content inside the scaffold.
 */
@OptIn(
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun PhovoNavigationSuiteScaffold(
    navigationSuiteItems: PhovoNavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    content: @Composable () -> Unit,
) {
    val layoutType = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> NavigationSuiteType.NavigationBar
        WindowWidthSizeClass.MEDIUM -> NavigationSuiteType.NavigationRail
        WindowWidthSizeClass.EXPANDED -> NavigationSuiteType.NavigationDrawer
        else -> NavigationSuiteType.NavigationBar
    }

    val navigationSuiteItemColors = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = PhovoNavigationDefaults.navigationContentColor(),
            selectedTextColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = PhovoNavigationDefaults.navigationContentColor(),
            indicatorColor = PhovoNavigationDefaults.navigationIndicatorColor(),
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = PhovoNavigationDefaults.navigationContentColor(),
            selectedTextColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = PhovoNavigationDefaults.navigationContentColor(),
            indicatorColor = PhovoNavigationDefaults.navigationIndicatorColor(),
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = PhovoNavigationDefaults.navigationContentColor(),
            selectedTextColor = PhovoNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = PhovoNavigationDefaults.navigationContentColor(),
            selectedContainerColor = PhovoNavigationDefaults.navigationIndicatorColor(),
        ),
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            PhovoNavigationSuiteScope(
                navigationSuiteScope = this,
                navigationSuiteItemColors = navigationSuiteItemColors,
            ).run(navigationSuiteItems)
        },
        layoutType = layoutType,
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContentColor = PhovoNavigationDefaults.navigationContentColor(),
            navigationRailContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * A wrapper around [NavigationSuiteScope] to declare navigation items.
 */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
class PhovoNavigationSuiteScope internal constructor(
    private val navigationSuiteScope: NavigationSuiteScope,
    private val navigationSuiteItemColors: NavigationSuiteItemColors,
) {
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit,
        selectedIcon: @Composable () -> Unit = icon,
        label: @Composable (() -> Unit)? = null,
    ) = navigationSuiteScope.item(
        selected = selected,
        onClick = onClick,
        icon = {
            if (selected) {
                selectedIcon()
            } else {
                icon()
            }
        },
        label = label,
        colors = navigationSuiteItemColors,
        modifier = modifier,
    )
}

@Composable
fun PhovoNavigationBarPreview() {
    val items = listOf("For you", "Saved", "Interests")
    val icons = listOf(
        PhovoIcons.UpcomingBorder,
        PhovoIcons.BookmarksBorder,
        PhovoIcons.Grid3x3,
    )
    val selectedIcons = listOf(
        PhovoIcons.Upcoming,
        PhovoIcons.Bookmarks,
        PhovoIcons.Grid3x3,
    )

    PhovoTheme {
        PhovoNavigationBar {
            items.forEachIndexed { index, item ->
                PhovoNavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = item,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = selectedIcons[index],
                            contentDescription = item,
                        )
                    },
                    label = { Text(item) },
                    selected = index == 0,
                    onClick = { },
                )
            }
        }
    }
}

@Composable
fun PhovoNavigationRailPreview() {
    val items = listOf("For you", "Saved", "Interests")
    val icons = listOf(
        PhovoIcons.UpcomingBorder,
        PhovoIcons.BookmarksBorder,
        PhovoIcons.Grid3x3,
    )
    val selectedIcons = listOf(
        PhovoIcons.Upcoming,
        PhovoIcons.Bookmarks,
        PhovoIcons.Grid3x3,
    )

    PhovoTheme {
        PhovoNavigationRail {
            items.forEachIndexed { index, item ->
                PhovoNavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = item,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = selectedIcons[index],
                            contentDescription = item,
                        )
                    },
                    label = { Text(item) },
                    selected = index == 0,
                    onClick = { },
                )
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