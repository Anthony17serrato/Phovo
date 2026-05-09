package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import com.serratocreations.phovo.feature.photos.ui.components.LoadMultiResImage
import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.ThumbnailPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.components.VideoPlayer
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

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
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        // Automatically request focus when the image is displayed. This assumes there
                        // is only one zoomable image present in the hierarchy. If you're displaying
                        // multiple images in a pager, apply this only for the active page.
                        focusRequester.requestFocus()
                    }

                    LoadMultiResImage(
                        lowRes = item.lowResThumbnail,
                        highRes = item.thumbnail,
                        sourceRes = item.sourceAsset,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = sharedElementTransition
                                    .rememberSharedContentState(key = "image-$key"),
                                animatedVisibilityScope = animatedContentScope
                            )
                            .focusRequester(focusRequester)
                            .zoomable(rememberZoomableState())
                            .fillMaxSize()
                    )
                }

                is VideoPhotoUiItem -> {
                    // TODO Support both local and remote video
                    if (item.sourceAsset is DomainAssetLocation.LocalAssetLocation) {
                        VideoPlayer(
                            videoPlatformFile = item.sourceAsset.localAssetLocation,
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
}
