package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val state by photosViewModel.photosUiState.collectAsStateWithLifecycle()
    val photos = remember(state.photosFeed) {
        state.photosFeed.filterIsInstance<ThumbnailPhotoUiItem>()
    }
    val currentSelectedPhoto = state.selectedPhoto

    if (photos.isEmpty() || currentSelectedPhoto == null) return

    val initialPage = remember(photos) {
        photos.indexOf(currentSelectedPhoto).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(initialPage = initialPage) {
        photos.size
    }

    // Sync ViewModel selectedPhoto -> Pager selection (for external changes)
    LaunchedEffect(currentSelectedPhoto) {
        val targetPage = photos.indexOf(currentSelectedPhoto)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // Sync Pager selection -> ViewModel selectedPhoto (for swipes)
    LaunchedEffect(pagerState.currentPage) {
        val activePhoto = photos.getOrNull(pagerState.currentPage)
        if (activePhoto != null && activePhoto != photosViewModel.photosUiState.value.selectedPhoto) {
            photosViewModel.onPhotoClick(activePhoto)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        pageSpacing = 16.dp
    ) { page ->
        val photo = photos.getOrNull(page)
        PhotoViewerScreen(
            item = photo,
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = animatedContentScope,
            areBarsVisible = areBarsVisible,
            onToggleBars = onToggleBars,
            isActivePage = (page == pagerState.currentPage),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotoViewerScreen(
    item: ThumbnailPhotoUiItem?,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    areBarsVisible: Boolean,
    onToggleBars: () -> Unit,
    isActivePage: Boolean,
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
                    LaunchedEffect(isActivePage) {
                        if (isActivePage) {
                            focusRequester.requestFocus()
                        }
                    }

                    LoadMultiResImage(
                        lowRes = item.lowResThumbnail,
                        highRes = item.thumbnail,
                        sourceRes = item.sourceAsset,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .sharedBounds(
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
                        if (isActivePage) {
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
                        } else {
                            // Show static thumbnail for non-active video pages
                            LoadMultiResImage(
                                lowRes = item.lowResThumbnail,
                                highRes = item.thumbnail,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .sharedBounds(
                                        sharedContentState = sharedElementTransition
                                            .rememberSharedContentState(key = "image-$key"),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onToggleBars
                                    )
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

