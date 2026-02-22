package com.serratocreations.phovo.core.designsystem.model

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource

sealed interface IconAsset

data class ImageVectorIcon(
    val icon: ImageVector
): IconAsset

data class PainterVectorIcon(
    val icon: DrawableResource
): IconAsset