package com.serratocreations.phovo.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

data class AppBarConfig(
    val title: @Composable (() -> Unit) = {},
    val navigationIcon: @Composable (() -> Unit) = { NoNavigationIcon() }
)

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