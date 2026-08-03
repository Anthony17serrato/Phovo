package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun WelcomeBottomSheet(
    onProceedWelcomeBottomSheet: () -> Unit,
    shouldShowBottomSheet: Boolean
)