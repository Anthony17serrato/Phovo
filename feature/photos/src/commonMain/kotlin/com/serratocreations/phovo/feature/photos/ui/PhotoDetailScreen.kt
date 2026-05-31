package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.serratocreations.phovo.feature.photos.ui.components.SystemBarsController
import com.serratocreations.phovo.feature.photos.util.CycleZoomOnDoubleClick
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
internal fun PhotoViewerScreen(
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    photosViewModel: PhotosViewModel,
    areBarsVisible: Boolean,
    onToggleBars: () -> Unit,
    modifier: Modifier = Modifier
) {
    PhotoViewerScreen(
        item = photosViewModel.photosUiState.value.selectedPhoto,
        sharedElementTransition = sharedElementTransition,
        animatedContentScope = animatedContentScope,
        areBarsVisible = areBarsVisible,
        onToggleBars = onToggleBars,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoViewerScreen(
    item: ThumbnailPhotoUiItem?,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    areBarsVisible: Boolean,
    onToggleBars: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return
    SystemBarsController(visible = areBarsVisible)
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
                            .zoomable(
                                state = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 3f)),
                                onClick = { _ -> onToggleBars() },
                                onDoubleClick = CycleZoomOnDoubleClick(onDoubleClick = { onToggleBars() })
                            )
                            .fillMaxSize()
                    )
                }

                is VideoPhotoUiItem -> {
                    // TODO Support both local and remote video
                    if (item.sourceAsset is DomainAssetLocation.LocalAssetLocation) {
                        VideoPlayer(
                            videoPlatformFile = item.sourceAsset.localAssetLocation,
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = sharedElementTransition
                                        .rememberSharedContentState(key = "image-$key"),
                                    animatedVisibilityScope = animatedContentScope
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onToggleBars
                                )
                        )
                    }
                }
            }
        }
    }
}
