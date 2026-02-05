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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.UriPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.reusablecomponents.VideoPlayer
import com.serratocreations.phovo.feature.photos.util.SetStatusBarAppearance

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoViewerScreen(
    onBackClick: () -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    photosViewModel: PhotosViewModel,
    modifier: Modifier = Modifier
) {
    PhotoViewerScreen(
        item = photosViewModel.photosUiState.value.selectedPhoto,
        onBackClick = onBackClick,
        sharedElementTransition = sharedElementTransition,
        animatedContentScope = animatedContentScope,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoViewerScreen(
    item: UriPhotoUiItem?,
    onBackClick: () -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) = with(sharedElementTransition) {
    if (item == null) return@with
    SetStatusBarAppearance(lightIcons = true)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val uri = item.uri.toString()
        when(item) {
            is ImagePhotoUiItem -> {
                // Image is placed first so it's drawn behind the top bar
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.sharedElement(
                        sharedContentState = sharedElementTransition
                            .rememberSharedContentState(key = "image-$uri"),
                        animatedVisibilityScope = animatedContentScope
                    )
                )
            }
            is VideoPhotoUiItem -> {
                VideoPlayer(
                    videoUri = item.uri,
                    modifier = Modifier.sharedElement(
                        sharedContentState = sharedElementTransition
                            .rememberSharedContentState(key = "image-$uri"),
                        animatedVisibilityScope = animatedContentScope
                    )
                )
            }
        }

        with(animatedContentScope) {
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
                    )
                    .align(Alignment.TopStart) // Position at the top of the screen
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f), // Start with semi-transparent black
                                Color.Transparent // Fade to transparent
                            )
                        )
                    )
            )
        }
    }
}
