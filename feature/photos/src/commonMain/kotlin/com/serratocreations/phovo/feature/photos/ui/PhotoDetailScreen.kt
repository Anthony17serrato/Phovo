package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.UriPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.components.VideoPlayer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoViewerScreen(
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    photosViewModel: PhotosViewModel,
    modifier: Modifier = Modifier
) {

    PhotoViewerScreen(
        item = photosViewModel.photosUiState.value.selectedPhoto,
        sharedElementTransition = sharedElementTransition,
        animatedContentScope = animatedContentScope,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoViewerScreen(
    item: UriPhotoUiItem?,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    if (item == null) return
    Column(modifier = modifier.fillMaxSize()) {
        with(sharedElementTransition) {
            val uri = item.uri.toString()
            when (item) {
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
        }
    }
}
