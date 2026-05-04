package com.serratocreations.phovo.feature.photos.extensions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior

@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarScrollBehavior.hideAppBar() {
    state.heightOffset = state.heightOffsetLimit
}

@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarScrollBehavior.showAppBar() {
    state.heightOffset = 0f
}