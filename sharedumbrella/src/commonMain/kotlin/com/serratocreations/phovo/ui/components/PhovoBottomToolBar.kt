package com.serratocreations.phovo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import org.jetbrains.compose.resources.painterResource
import phovo.sharedumbrella.generated.resources.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.serratocreations.phovo.core.navigation.AppBarConfig

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhovoBottomToolBar(
    appBarConfig: AppBarConfig,
    modifier: Modifier = Modifier,
) {
//    val exitAlwaysScrollBehavior =
//        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
    AnimatedVisibility(
        visible = appBarConfig.showBottomToolbar,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier/*.align(Alignment.BottomCenter)*/
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            ),
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalFloatingToolbar(
                // Always expanded as the toolbar is bottom-centered.
                expanded = true,
                modifier =
                    Modifier/*.align(Alignment.BottomCenter)*/.offset(y = -ScreenOffset).zIndex(1f),
                // Match PhovoNavigationBar on the photos grid.
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                //scrollBehavior = exitAlwaysScrollBehavior,
                content = {
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text("Localized description") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(painterResource(Res.drawable.ic_person_default), contentDescription = "Localized description")
                        }
                    }
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text("Localized description") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(painterResource(Res.drawable.ic_edit_default), contentDescription = "Localized description")
                        }
                    }
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text("Localized description") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                painterResource(Res.drawable.ic_favorite_default),
                                contentDescription = "Localized description",
                            )
                        }
                    }
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text("Localized description") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                painterResource(Res.drawable.ic_more_vert_default),
                                contentDescription = "Localized description",
                            )
                        }
                    }
                },
            )
        }
    }
}