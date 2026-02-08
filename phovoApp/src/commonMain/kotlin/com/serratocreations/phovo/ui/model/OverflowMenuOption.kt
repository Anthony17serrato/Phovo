package com.serratocreations.phovo.ui.model

import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.StringResource

data class OverflowMenuOption(
    val title: StringResource,
    val route: NavKey
)