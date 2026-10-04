package com.serratocreations.phovo.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIScreen
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarDelegateProtocol
import platform.UIKit.UITabBarItem
import platform.darwin.NSObject

/**
 * iOS 26 draws [UITabBar] in Liquid Glass, so from there on the compact layout uses the system tab
 * bar instead of the Material one. Older releases keep [PhovoNavigationBar], since their tab bar is
 * the full-width bar that does not suit a floating layout.
 */
internal actual val platformNavigationBar:
    (@Composable (items: List<PhovoNavigationSuiteItem>, modifier: Modifier) -> Unit)? =
    if (supportsLiquidGlass()) {
        { items, modifier -> LiquidGlassTabBar(items, modifier) }
    } else {
        null
    }

@OptIn(ExperimentalForeignApi::class)
private fun supportsLiquidGlass(): Boolean =
    NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
        cValue<NSOperatingSystemVersion> {
            majorVersion = 26
            minorVersion = 0
            patchVersion = 0
        }
    )

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalForeignApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun LiquidGlassTabBar(
    items: List<PhovoNavigationSuiteItem>,
    modifier: Modifier,
) {
    val currentItems by rememberUpdatedState(items)
    val delegate = remember {
        TabBarDelegate { index -> currentItems.getOrNull(index)?.onClick?.invoke() }
    }
    val tabBar = remember { UITabBar().apply { this.delegate = delegate } }
    // The bar picks its layout from its own height: given less than it asks for, it falls back to
    // the compact platter that puts the label beside the icon. Its fitting height already includes
    // the home indicator, which it keeps clear of itself as long as it reaches the bottom edge.
    val barHeight = remember(tabBar) {
        tabBar.sizeThatFits(CGSizeMake(UIScreen.mainScreen.bounds.useContents { size.width }, 0.0))
            .useContents { height }.dp
    }
    val bottomInset = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val tint = MaterialTheme.colorScheme.primary.toUIColor()

    Box(Modifier.fillMaxWidth().height(barHeight)) {
        UIKitView(
            factory = { tabBar },
            update = { bar ->
                bar.tintColor = tint
                bar.sync(items)
            },
            modifier = Modifier.matchParentSize(),
            properties = UIKitInteropProperties(
                // Touches go straight to the tab bar so its press and drag between tabs feel native.
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
                // Above the Compose canvas, so the glass has the photos beneath it to refract. Placed
                // behind, it would only see the empty window through the hole Compose leaves for it.
                placedAsOverlay = true,
            ),
        )
        // Stands in for the part of the bar above the home indicator when reporting its height.
        Spacer(
            modifier
                .fillMaxWidth()
                .height(
                    (barHeight - bottomInset - FloatingToolbarDefaults.ScreenOffset)
                        .coerceAtLeast(0.dp)
                )
        )
    }
}

/**
 * Brings the bar's items in line with [items]. The [UITabBarItem]s are only rebuilt when the set of
 * destinations changes, so a selection change animates instead of resetting the bar.
 */
private fun UITabBar.sync(items: List<PhovoNavigationSuiteItem>) {
    @Suppress("UNCHECKED_CAST")
    var barItems = this.items as List<UITabBarItem>? ?: emptyList()
    val titlesMatch = barItems.map { it.title } == items.map { it.title }
    if (!titlesMatch) {
        barItems = items.map { item ->
            UITabBarItem(
                title = item.title,
                image = item.iosImage?.let(::namedImage),
                selectedImage = item.iosSelectedImage?.let(::namedImage),
            )
        }
        setItems(barItems, animated = false)
    }

    items.zip(barItems).forEach { (item, barItem) ->
        val badgeColor = item.badgeColor
        // An empty badge draws as a dot, matching the Material bar's status dot.
        barItem.badgeValue = if (badgeColor != null) "" else null
        barItem.badgeColor = badgeColor?.toUIColor()
    }

    val selected = barItems.getOrNull(items.indexOfFirst { it.selected })
    if (selectedItem != selected) selectedItem = selected
}

/** An image from the app's asset catalog, falling back to the SF Symbol of that name. */
private fun namedImage(name: String): UIImage? =
    UIImage.imageNamed(name) ?: UIImage.systemImageNamed(name)

private class TabBarDelegate(
    private val onSelect: (index: Int) -> Unit,
) : NSObject(), UITabBarDelegateProtocol {
    override fun tabBar(tabBar: UITabBar, didSelectItem: UITabBarItem) {
        val index = tabBar.items?.indexOf(didSelectItem) ?: return
        if (index >= 0) onSelect(index)
    }
}

private fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)

