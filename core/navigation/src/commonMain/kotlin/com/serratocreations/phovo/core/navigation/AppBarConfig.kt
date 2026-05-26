package com.serratocreations.phovo.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.common.ui.MEDIUM_WIDTH

private val BOTTOM_APP_BAR_HEIGHT = 80.dp
@Immutable
data class AppBarConfig(
    val title: @Composable (() -> Unit) = {},
    val navigationIcon: @Composable (() -> Unit) = { NoNavigationIcon() },
    val topAppBarColors: @Composable (() -> TopAppBarColors) = { TopAppBarDefaults.topAppBarColors() },
    val shouldOverlayTopAppBar: Boolean = false,
    val showBottomAppBar: Boolean = true
) {
    @Composable
    fun calculateAdjustedPadding(scaffoldPadding: PaddingValues): PaddingValues {
        val isCompact = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(MEDIUM_WIDTH).not()
        val layoutDirection = LocalLayoutDirection.current

        return PaddingValues(
            start = scaffoldPadding.calculateStartPadding(layoutDirection),
            top = if (shouldOverlayTopAppBar) 0.dp else scaffoldPadding.calculateTopPadding(),
            end = scaffoldPadding.calculateEndPadding(layoutDirection),
            bottom = scaffoldPadding.calculateBottomPadding() + if (isCompact && showBottomAppBar) BOTTOM_APP_BAR_HEIGHT else 0.dp
        )
    }
}

@Composable
fun DefaultNavigationIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            // TODO Extract string resource
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NoNavigationIcon() {

}