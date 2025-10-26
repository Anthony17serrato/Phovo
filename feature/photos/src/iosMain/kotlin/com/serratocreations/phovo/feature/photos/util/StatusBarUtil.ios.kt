package com.serratocreations.phovo.feature.photos.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@Composable
actual fun SetStatusBarAppearance(lightIcons: Boolean) {
    val app = UIApplication.sharedApplication
    val originalStyle = remember { app.statusBarStyle }

    DisposableEffect(lightIcons) {
        val newStyle = if (lightIcons) UIStatusBarStyleLightContent
        else UIStatusBarStyleDarkContent
        app.setStatusBarStyle(newStyle)

        onDispose {
            app.setStatusBarStyle(originalStyle)
        }
    }
}