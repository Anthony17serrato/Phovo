package com.serratocreations.phovo.feature.photos.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.UriPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.util.getPlatformDecoderFactory
import com.serratocreations.phovo.feature.photos.util.getPlatformFetcherFactory

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotosRoute(
    onPhotoClick: (UriPhotoUiItem) -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    photosViewModel: PhotosViewModel,
    modifier: Modifier = Modifier,
) {
    // TODO move to root composable https://coil-kt.github.io/coil/image_loaders/
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(getPlatformDecoderFactory())
                add(getPlatformFetcherFactory())
            }
            .build()
    }
    val photosState by photosViewModel.photosUiState.collectAsStateWithLifecycle()
    PhotosScreen(
        photosItems = photosState.photosFeed,
        onPhotoClick = onPhotoClick,
        sharedElementTransition =sharedElementTransition,
        animatedContentScope = animatedContentScope,
        modifier = modifier
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun PhotosScreen(
    photosItems: List<PhotoUiItem>,
    onPhotoClick: (UriPhotoUiItem) -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    width: WindowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(
                minSize = when (width) {
                    WindowWidthSizeClass.COMPACT -> 80.dp
                    WindowWidthSizeClass.MEDIUM -> 120.dp
                    WindowWidthSizeClass.EXPANDED -> 160.dp
                    else -> 80.dp
                }
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                CallToActionComponent(
                    actionTitle = "Finish setup",
                    actionDescription = "Get more from your gallery",
                    onClick = { /* TODO */ }
                )
            }
            itemsIndexed(
                items = photosItems,
                span = { index, item ->
                    when (item) {
                        is DateHeaderPhotoUiItem -> {
                            GridItemSpan(maxLineSpan)
                        }
                        is UriPhotoUiItem -> {
                            GridItemSpan(1)
                        }
                    }
                }
            ) { index, item ->
                when (item) {
                    is DateHeaderPhotoUiItem -> {
                        Text(
                            // TODO: month must come from a localized string, consider an enum class with
                            //  string res values
                            text = "${item.month} ${item.year ?: ""}",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = modifier.padding(16.dp)
                        )
                    }
                    is UriPhotoUiItem -> with(sharedElementTransition) {
                        val id = item.uri.toString()
                        Box {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = modifier
                                    .aspectRatio(1f)
                                    .sharedElement(
                                        sharedContentState = sharedElementTransition
                                            .rememberSharedContentState(key = "image-$id"),
                                        animatedVisibilityScope = animatedContentScope
                                    ).clickable { onPhotoClick(item) }
                            )
                            if (item is VideoPhotoUiItem) {
                                Text(
                                    text = item.duration,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = modifier.align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}