package com.serratocreations.phovo.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhovoTopAppBar(
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String,
    actionIcon: ImageVector,
    actionIconContentDescription: String,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    expandableComponent: @Composable () -> Unit
) {
    TopAppBar(
        title = expandableComponent,
//        navigationIcon = {
//            AnimatedVisibility(navigationIcon != null) {
//                navigationIcon?.let { iconNotNull ->
//                    IconButton(onClick = onNavigationClick) {
//                        Icon(
//                            imageVector = iconNotNull,
//                            contentDescription = navigationIconContentDescription,
//                            tint = MaterialTheme.colorScheme.onSurface,
//                        )
//                    }
//                }
//            }
//        },
        actions = {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = colors,
        modifier = modifier.testTag("niaTopAppBar"),
        scrollBehavior = scrollBehavior
    )
//    LargeFlexibleTopAppBar(
//        title = expandableComponent,
//        titleHorizontalAlignment = Alignment.Start,
//        actions = {
//            TooltipBox(
//                positionProvider =
//                    TooltipDefaults.rememberTooltipPositionProvider(
//                        TooltipAnchorPosition.Above
//                    ),
//                tooltip = { PlainTooltip { Text("Settings") } },
//                state = rememberTooltipState(),
//            ) {
//                IconButton(onClick = onActionClick) {
//                    Icon(
//                        imageVector = actionIcon,
//                        contentDescription = actionIconContentDescription,
//                        tint = MaterialTheme.colorScheme.onSurface,
//                    )
//                }
//            }
//        },
//        scrollBehavior = scrollBehavior,
//    )
}