package com.serratocreations.phovo.core.designsystem.util

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.serratocreations.phovo.core.common.ui.PhovoPaneMode

val WindowAdaptiveInfo.getPaneMode: PhovoPaneMode
    get() = when (this.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.EXPANDED -> PhovoPaneMode.TwoPane
        else -> PhovoPaneMode.SinglePane
    }