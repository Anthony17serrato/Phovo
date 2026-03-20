package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.ThumbnailPhotoUiItem
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
    item: ThumbnailPhotoUiItem?,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    if (item == null) return
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        with(sharedElementTransition) {
            val key = item.key
            when (item) {
                is ImagePhotoUiItem -> {
                    Box {
                        var isSourceQualityImageLoaded by remember { mutableStateOf(false) }
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onSuccess = {
                                isSourceQualityImageLoaded = true
                            },
                            modifier = Modifier.sharedElement(
                                sharedContentState = sharedElementTransition
                                    .rememberSharedContentState(key = "image-$key"),
                                animatedVisibilityScope = animatedContentScope
                            ).fillMaxSize()
                        )
                        var isHighResThumbLoaded by remember { mutableStateOf(false) }
                        Column {
                            AnimatedVisibility(
                                visible = isSourceQualityImageLoaded.not(),
                                exit = fadeOut()
                            ) {
                                AsyncImage(
                                    model = item.thumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    onSuccess = {
                                        isHighResThumbLoaded = true
                                    },
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = sharedElementTransition
                                            .rememberSharedContentState(key = "image-$key"),
                                        animatedVisibilityScope = animatedContentScope
                                    ).fillMaxSize()
                                )
                            }

                        }
                        Column {
                            AnimatedVisibility(
                                visible = isSourceQualityImageLoaded.not() && isHighResThumbLoaded.not(),
                                exit = fadeOut()
                            ) {
                                AsyncImage(
                                    model = item.lowResThumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = sharedElementTransition
                                            .rememberSharedContentState(key = "image-$key"),
                                        animatedVisibilityScope = animatedContentScope
                                    ).fillMaxSize()
                                )
                            }
                        }
                    }
                }

                is VideoPhotoUiItem -> {
                    VideoPlayer(
                        videoUri = item.uri,
                        modifier = Modifier.sharedElement(
                            sharedContentState = sharedElementTransition
                                .rememberSharedContentState(key = "image-$key"),
                            animatedVisibilityScope = animatedContentScope
                        )
                    )
                }
            }
        }
    }
}
