package com.serratocreations.phovo.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.ui.model.OverflowMenuOption
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhovoTopAppBar(
    appBarState: AppBarConfig,
    actionIcon: ImageVector,
    actionIconContentDescription: String,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    menuOptions: Set<OverflowMenuOption>,
    onMenuActionClick: (NavKey) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior
) {
    var expanded by remember { mutableStateOf(false) }
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
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        )
                    }
                }
            }
        },
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}