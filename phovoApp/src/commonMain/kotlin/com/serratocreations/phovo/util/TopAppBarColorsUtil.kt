package com.serratocreations.phovo.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarColors.animated(
    animationSpec: AnimationSpec<Color> = tween(durationMillis = 300)
): TopAppBarColors {
    val container by animateColorAsState(containerColor, animationSpec, "container")
    val scrolledContainer by animateColorAsState(scrolledContainerColor, animationSpec, "scrolledContainer")
    val navIcon by animateColorAsState(navigationIconContentColor, animationSpec, "navIcon")
    val title by animateColorAsState(titleContentColor, animationSpec, "title")
    val actionIcon by animateColorAsState(actionIconContentColor, animationSpec, "actionIcon")

    return TopAppBarDefaults.topAppBarColors(
        containerColor = container,
        scrolledContainerColor = scrolledContainer,
        navigationIconContentColor = navIcon,
        titleContentColor = title,
        actionIconContentColor = actionIcon
    )
}