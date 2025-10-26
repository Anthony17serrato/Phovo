package com.serratocreations.phovo.feature.photos.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetStatusBarAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return

    // Remember the original status bar state
    val originalLightStatusBars = remember {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars
    }

    DisposableEffect(lightIcons) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !lightIcons

        onDispose {
            controller.isAppearanceLightStatusBars = originalLightStatusBars
        }
    }
}