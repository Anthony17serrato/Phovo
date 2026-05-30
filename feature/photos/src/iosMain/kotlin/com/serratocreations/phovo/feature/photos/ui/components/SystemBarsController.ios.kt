package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.setStatusBarHidden

@Composable
actual fun SystemBarsController(visible: Boolean) {
    DisposableEffect(visible) {
        UIApplication.sharedApplication.setStatusBarHidden(!visible, withAnimation = UIStatusBarAnimation.UIStatusBarAnimationFade)
        onDispose {
            UIApplication.sharedApplication.setStatusBarHidden(false, withAnimation = UIStatusBarAnimation.UIStatusBarAnimationFade)
        }
    }
}
