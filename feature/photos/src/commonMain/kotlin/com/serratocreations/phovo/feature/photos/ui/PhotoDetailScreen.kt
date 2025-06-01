package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun PhotoDetailRoute(
    uri: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PhotoDetailScreen(
        uri = uri,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoDetailScreen(
    uri: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Image is placed first so it's drawn behind the top bar
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Add a gradient overlay for the top bar area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp) // Standard height for TopAppBar
                .align(Alignment.TopStart) // Position at the top of the screen
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f), // Start with semi-transparent black
                            Color.Transparent // Fade to transparent
                        )
                    )
                )
        )

        // Scaffold with transparent top bar is placed on top of the image
        Scaffold(
            containerColor = Color.Transparent, // Make scaffold background transparent
            contentColor = Color.White, // Set content color to ensure visibility
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White // Ensure the icon is visible against any background
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make top bar background transparent
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            // Empty content as we've already placed our content in the Box
        }
    }
}
