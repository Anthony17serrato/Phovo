package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import coil3.compose.AsyncImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoDetailRoute(
    uri: String,
    onBackClick: () -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    PhotoDetailScreen(
        uri = uri,
        onBackClick = onBackClick,
        sharedElementTransition =sharedElementTransition,
        animatedContentScope = animatedContentScope,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoDetailScreen(
    uri: String,
    onBackClick: () -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) = with(sharedElementTransition) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Image is placed first so it's drawn behind the top bar
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().sharedElement(
                sharedContentState = sharedElementTransition
                    .rememberSharedContentState(key = "image-$uri"),
                animatedVisibilityScope = animatedContentScope
            )
        )

        with(animatedContentScope) {
            // Scaffold with transparent gradient bar is placed on top of the image
            Scaffold(
                modifier = Modifier
                    .fillMaxWidth(),
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
                        ),
                        modifier = Modifier
                            .renderInSharedTransitionScopeOverlay()
                            .animateEnterExit(
                                enter = fadeIn() + slideInVertically {
                                    -it
                                },
                                exit = fadeOut() + slideOutVertically {
                                    -it
                                }
                            ).background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f), // Start with semi-transparent black
                                        Color.Transparent // Fade to transparent
                                    )
                                )
                            )
                    )
                }
            ) { paddingValues ->
                // TODO: place actual screen content here instead of in box
                // Empty content as we've already placed our content in the Box
            }
        }
    }
}
