package com.serratocreations.phovo.feature.connections.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import phovo.feature.connections.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ConfigGettingStartedScreen(
    connectionsViewModel: ConnectionsViewModel,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(end = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
    ) {
        Column(modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
            AsyncImage(
                model = Res.getUri("drawable/cloud_backup.png"),
                modifier = Modifier.height(350.dp),
                contentScale = ContentScale.FillHeight,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {  }) {
                Text("Turn on backup")
            }
            TextButton(onClick = {  }) {
                Text("No thanks")
            }
        }
    }
}