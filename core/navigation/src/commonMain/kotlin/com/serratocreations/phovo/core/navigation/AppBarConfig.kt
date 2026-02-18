package com.serratocreations.phovo.core.navigation

import androidx.compose.runtime.Composable

data class AppBarConfig(
    val title: @Composable (() -> Unit) = {},
    val navigationIcon: @Composable (() -> Unit) = {}
)
