package com.serratocreations.phovo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import org.jetbrains.compose.resources.painterResource
import phovo.sharedumbrella.generated.resources.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.ExpandableHeaderDefaults
import com.serratocreations.phovo.core.navigation.ExpandableHeaderLayout
import com.serratocreations.phovo.core.navigation.expandableHeaderBackground
import com.serratocreations.phovo.ui.model.OverflowMenuOption
import com.serratocreations.phovo.util.animated
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhovoTopAppBar(
    appBarState: AppBarConfig,
    actionIcon: ImageVector,
    actionIconContentDescription: String,
    modifier: Modifier = Modifier,
    menuOptions: Set<OverflowMenuOption>,
    onMenuActionClick: (NavKey) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior
) {
    var expanded by remember { mutableStateOf(false) }
    val animatedAppBarColors = appBarState.topAppBarColors().animated()
    AnimatedVisibility(
        visible = appBarState.showTopAppBar,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        val header = appBarState.expandableHeader
        val headerColor = ExpandableHeaderDefaults.containerColor
        Column(
            modifier = modifier.then(
                header?.let { Modifier.expandableHeaderBackground(it.state, headerColor) }
                    ?: Modifier
            )
        ) {
            TopAppBar(
                title = appBarState.title,
                navigationIcon = appBarState.navigationIcon,
                actions = {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        // Icon button should have a tooltip associated with it for a11y.
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Localized description") } },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = actionIcon,
                                    contentDescription = actionIconContentDescription,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            menuOptions.forEach { menuOption ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(menuOption.title)) },
                                    onClick = {
                                        expanded = false
                                        onMenuActionClick(menuOption.route)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(menuOption.icon),
                                            contentDescription = null
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
                colors = animatedAppBarColors,
                scrollBehavior = scrollBehavior
            )
            // Under the bar's row rather than in its title slot, so it takes the full width and the
            // actions keep their place.
            if (header != null) {
                ExpandableHeaderLayout(
                    header = header,
                    modifier = Modifier.padding(horizontal = ExpandableHeaderHorizontalPadding)
                )
            }
        }
    }
}

/** Lines the header up with the bar's title. */
private val ExpandableHeaderHorizontalPadding = 16.dp
