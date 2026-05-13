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
    modifier: Modifier = Modifier
) {
    val lowPainter = lowRes?.let {
        val thumbRequest = ImageRequest.Builder(LocalPlatformContext.current)
            .data(it)
            .size(144, 144)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .memoryCacheKey("low_thumb_${it.hashCode()}")
            .diskCacheKey("low_thumb_${it.hashCode()}")
            .build()
        rememberAsyncImagePainter(model = thumbRequest)
    }
    val highPainter = highRes?.let {
        val thumbRequest = ImageRequest.Builder(LocalPlatformContext.current)
            .data(it)
            .size(720, 720)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .memoryCacheKey("high_thumb_${it.hashCode()}")
            .diskCacheKey("high_thumb_${it.hashCode()}")
            .build()
        rememberAsyncImagePainter(thumbRequest)
    }
    val sourcePainter = sourceRes?.let { rememberAsyncImagePainter(it) }

    val lowState = lowPainter?.state?.collectAsState()?.value
    val highState = highPainter?.state?.collectAsState()?.value
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
                // Note: The shared element modifier is no longer here.
                // We only apply the blur to the inner image.
                modifier = Modifier
                    .fillMaxSize()
                    .letIf(state.shouldShowBlur) { m ->
                        m.blur(10.dp)
                    }
            )
        }
    }
}