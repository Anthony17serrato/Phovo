package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import phovo.feature.connections.generated.resources.Res
import phovo.feature.connections.generated.resources.cloud_sync

@Composable
internal fun ConfigGettingStartedPane(
    onClickBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Height of the pinned call-to-action block, reserved at the end of the scrolling content so
    // the illustration can be scrolled fully clear of it.
    var ctaBlockHeight by remember { mutableStateOf(0.dp) }
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier.padding(end = 16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // TODO extract string resources
                Text(
                    text = "Keep your photos & videos safe with Phovo",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Access your photos from any device, and unlock powerful features. By configuring this device as a Phovo server images and videos will be backed up to this device and accessible on all of your devices.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You can turn off backup any time in settings.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Image(
                    painter = painterResource(Res.drawable.cloud_sync),
                    // Square, up to 455dp, shrinking with the pane rather than cropping.
                    modifier = Modifier
                        .widthIn(max = 455.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(ctaBlockHeight))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        ctaBlockHeight = with(density) { size.height.toDp() }
                    }
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to containerColor,
                            1f to containerColor
                        )
                    )
                    // Fade runway so partially faded content reads as "there is more to scroll".
                    .padding(top = 48.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = onClickBackup) {
                    Text("Turn on backup")
                }
                TextButton(onClick = { }) {
                    Text("No thanks")
                }
            }
        }
    }
}
