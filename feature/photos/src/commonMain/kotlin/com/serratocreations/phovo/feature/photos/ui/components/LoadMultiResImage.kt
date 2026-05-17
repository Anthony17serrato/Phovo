package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import com.serratocreations.phovo.core.common.util.letIf
import com.serratocreations.phovo.core.domain.model.DomainAssetLocation

// Keeps the image and its blur state bundled so AnimatedContent transitions them together
private data class ImageLoadState(
    val painter: AsyncImagePainter,
    val shouldShowBlur: Boolean
)

@Composable
fun LoadMultiResImage(
    lowRes: DomainAssetLocation? = null,
    highRes: DomainAssetLocation? = null,
    sourceRes: DomainAssetLocation? = null,
    contentScale: ContentScale,
    shouldLoadSequentially: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lowPainter = lowRes?.let {
        val thumbRequest = ImageRequest.Builder(LocalPlatformContext.current)
            .data(lowRes)
            .size(144, 144)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .memoryCacheKey("low_thumb_${lowRes.assetId}")
            .diskCacheKey("low_thumb_${lowRes.assetId}")
            .build()
        rememberAsyncImagePainter(model = thumbRequest)
    }

    val lowState = lowPainter?.state?.collectAsState()?.value

    // A step is "finished" if it was skipped (null) OR if it completed (Success/Error)
    // We include Error so a failed thumbnail doesn't halt the whole loading chain.
    val isLowFinished = lowRes == null || lowState is AsyncImagePainter.State.Success || lowState is AsyncImagePainter.State.Error
    val canLoadHigh = !shouldLoadSequentially || isLowFinished

    // 2. HIGH RES
    val highPainter = if (canLoadHigh && highRes != null) {
        val thumbRequest = ImageRequest.Builder(LocalPlatformContext.current)
            .data(highRes)
            .size(720, 720)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .memoryCacheKey("high_thumb_${highRes.assetId}")
            .diskCacheKey("high_thumb_${highRes.assetId}")
            .build()
        rememberAsyncImagePainter(thumbRequest)
    } else null

    val highState = highPainter?.state?.collectAsState()?.value

    val isHighFinished = highRes == null || highState is AsyncImagePainter.State.Success || highState is AsyncImagePainter.State.Error
    val canLoadSource = !shouldLoadSequentially || (isLowFinished && isHighFinished)

    val sourcePainter = if (canLoadSource && sourceRes != null) {
        rememberAsyncImagePainter(sourceRes)
    } else null

    val sourceState = sourcePainter?.state?.collectAsState()?.value

    val targetImageState = when {
        sourceState is AsyncImagePainter.State.Success -> ImageLoadState(sourcePainter, false)
        highState is AsyncImagePainter.State.Success -> ImageLoadState(highPainter, false)
        lowState is AsyncImagePainter.State.Success -> ImageLoadState(lowPainter, true)
        else -> null
    }

    if (targetImageState != null) {
        AnimatedContent(
            targetState = targetImageState,
            // 1. Anchor the modifier here! This keeps the shared element stable(if used).
            modifier = modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) togetherWith ExitTransition.KeepUntilTransitionsFinished)
                    // 2. Disable internal size animations. Let the Shared Element handle bounds.
                    .using(null)
            },
            // 3. Keep everything perfectly centered if the raw image dimensions differ slightly
            contentAlignment = Alignment.Center,
            label = "MultiResImageTransition"
        ) { state ->
            Image(
                painter = state.painter,
                contentDescription = null,
                contentScale = contentScale,
                // We only apply the blur if the state requires it.
                modifier = Modifier
                    .fillMaxSize()
                    .letIf(state.shouldShowBlur) { m ->
                        m.blur(10.dp)
                    }
            )
        }
    }
}