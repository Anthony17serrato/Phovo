package com.serratocreations.phovo.feature.photos

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.feature.photos.util.getPlatformDecoderFactory
import com.serratocreations.phovo.feature.photos.util.getPlatformFetcherFactory
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ForYouRoute(
    modifier: Modifier = Modifier,
    phovoViewModel: PhovoViewModel = koinViewModel()
) {
    // TODO move to root composable https://coil-kt.github.io/coil/image_loaders/
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(getPlatformFetcherFactory())
                add(getPlatformDecoderFactory())
            }
            .build()
    }
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                FilledCardExample()
            }
            items(
                bookmarksState
            ) { photo ->
                println(photo.uri.toString())
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
fun FilledCardExample() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PhovoIcons.Info,
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp)
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    text = "Finish setup",
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Get more from your gallery",
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = PhovoIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}
