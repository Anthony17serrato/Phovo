package com.serratocreations.phovo.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serratocreations.phovo.data.db.entity.PhovoItem
import androidx.compose.foundation.lazy.items
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
        /**
         * import androidx.compose.foundation.lazy.items
         */
        LazyColumn {
            items(bookmarksState) { item ->
                Text(item.toString())
            }
        }
    }
}