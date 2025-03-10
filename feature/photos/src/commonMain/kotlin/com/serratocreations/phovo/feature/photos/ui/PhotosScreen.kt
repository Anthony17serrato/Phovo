package com.serratocreations.phovo.feature.photos.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.util.getPlatformFetcherFactory
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun PhotosRoute(
    modifier: Modifier = Modifier,
    photosViewModel: PhotosViewModel = koinViewModel()
) {
    // TODO move to root composable https://coil-kt.github.io/coil/image_loaders/
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(getPlatformFetcherFactory())
            }
            .build()
    }
    val photosState by photosViewModel.phovoUiState.collectAsStateWithLifecycle()
    PhotosScreen(
        photosState = photosState,
        modifier = modifier
    )
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun PhotosScreen(
    photosState: List<PhotoUiItem>,
    modifier: Modifier = Modifier,
    width: WindowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
            items(
                items = photosState,
                span = { item ->
                    when (item) {
                        is DateHeaderPhotoUiItem -> {
                            GridItemSpan(maxLineSpan)
                        }
                        is ImagePhotoUiItem -> {
                            GridItemSpan(1)
                        }
                    }
                }
            ) { item ->
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
                    is ImagePhotoUiItem -> {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = modifier.aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}
