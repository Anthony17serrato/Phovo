package com.serratocreations.phovo.feature.photos

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.serratocreations.phovo.feature.photos.util.HEICImageDecoderFactory
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ForYouRoute(
    modifier: Modifier = Modifier,
    phovoViewModel: PhovoViewModel = koinViewModel()
) {
    val bookmarksState by phovoViewModel.phovoUiState.collectAsStateWithLifecycle()
    ForYouScreen(
        bookmarksState = bookmarksState,
        modifier = modifier
    )
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun ForYouScreen(
    bookmarksState: List<PhovoItem>,
    modifier: Modifier = Modifier,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        //Text(bookmarksState.size.toString())
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                bookmarksState
            ) { photo ->
                val builder = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(photo.uri)
                println(photo.uri.path)
                if (photo.uri.path?.substringAfterLast('.').equals("HEIC", ignoreCase = true)) {
                    builder.decoderFactory { result, options, imageLoader ->
                        HEICImageDecoderFactory().create(result, options, imageLoader)
                    }
                }
                AsyncImage(
                    model = builder.build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.aspectRatio(1f)
                )
            }
        }
    }
}