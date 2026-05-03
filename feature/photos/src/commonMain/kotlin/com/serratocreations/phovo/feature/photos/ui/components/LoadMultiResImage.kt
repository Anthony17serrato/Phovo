package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.serratocreations.phovo.core.common.util.letIf
import com.serratocreations.phovo.core.domain.model.DomainAssetLocation

@Composable
fun LoadMultiResImage(
    lowRes: DomainAssetLocation? = null,
    highRes: DomainAssetLocation? = null,
    sourceRes: DomainAssetLocation? = null,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    val lowPainter = lowRes?.let { rememberAsyncImagePainter(it) }
    val highPainter = highRes?.let { rememberAsyncImagePainter(it) }
    val sourcePainter = sourceRes?.let { rememberAsyncImagePainter(it) }

    val lowState = lowPainter?.state?.collectAsState()?.value
    val highState = highPainter?.state?.collectAsState()?.value
    val sourceState = sourcePainter?.state?.collectAsState()?.value

    val (successfulPainter, shouldShowBlur) = when {
        sourceState is AsyncImagePainter.State.Success -> Pair(sourcePainter, false)
        highState is AsyncImagePainter.State.Success -> Pair(highPainter, false)
        lowState is AsyncImagePainter.State.Success -> Pair(lowPainter, true)
        else -> Pair(null, false)
    }

    if (successfulPainter != null) {
        Image(
            painter = successfulPainter,
            contentDescription = null, // In the future this may come from GenAI
            contentScale = contentScale,
            modifier = modifier
                .letIf(shouldShowBlur) { modifier ->
                    modifier.blur(10.dp)
                }
        )
    }
}