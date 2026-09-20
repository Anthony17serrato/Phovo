package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent

/**
 * The full list behind the feed's summary row: every call to action, highest priority first, with
 * the copy shown in full rather than truncated.
 */
@Composable
internal fun CallToActionsScreen(
    photosViewModel: PhotosViewModel,
    onCallToActionClick: (CallToActionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val photosState by photosViewModel.photosUiState.collectAsStateWithLifecycle()
    val ordered = remember(photosState.callToActions) {
        photosState.callToActions.sortedBy { it.priority }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ordered, key = { it.actionTitle }) { callToAction ->
            CallToActionComponent(
                actionTitle = callToAction.actionTitle,
                actionDescription = callToAction.actionDescription,
                onClick = { onCallToActionClick(callToAction.action) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
