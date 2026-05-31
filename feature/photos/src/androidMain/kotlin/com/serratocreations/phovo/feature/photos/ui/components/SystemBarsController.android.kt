package com.serratocreations.phovo.feature.photos.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun SystemBarsController(visible: Boolean) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window ?: return

    DisposableEffect(visible) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (!visible) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
