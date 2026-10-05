package com.serratocreations.phovo.feature.photos.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import com.serratocreations.phovo.feature.photos.ui.model.MediaUiItem

/**
 * The photo viewer's pages and pager, kept in step with a feed that changes while it is open.
 *
 * The pager tracks its position by index, so the pages and the current page have to change
 * together. While media is processed the feed re-emits several times a second with items inserted
 * anywhere, which the pager's own key matching only follows over short distances, and only one frame
 * after the new pages have already been composed. So the viewer holds its own copy of the pages and
 * swaps in the latest feed alongside an explicit move to the same photo.
 *
 * This lives in the UI rather than the view model because the swap and the move have to happen in
 * one snapshot, and the move needs the [PagerState], which belongs to the composition.
 */
@Stable
internal class PhotoViewerState(
    initialPhotos: List<MediaUiItem>,
    initialPhotoKey: String
) {
    var photos: List<MediaUiItem> by mutableStateOf(initialPhotos)
        private set

    val pagerState: PagerState = PagerState(
        currentPage = initialPhotos.indexOfFirst { it.key == initialPhotoKey }.coerceAtLeast(0)
    ) {
        photos.size
    }

    /** The key of the photo on the current page. */
    val activePhotoKey: String? by derivedStateOf { photos.getOrNull(pagerState.currentPage)?.key }

    /** Swaps in each new value of [latestPhotos] while keeping the same photo on screen. */
    suspend fun followFeed(latestPhotos: () -> List<MediaUiItem>) {
        snapshotFlow { latestPhotos() to pagerState.isScrollInProgress }
            .collect { (latest, isScrollInProgress) ->
                // Moving the pager cancels any scroll in progress, so an update that lands mid-swipe
                // waits for the swipe to settle instead of yanking the photo from under the finger.
                if (isScrollInProgress || latest === photos) return@collect
                val page = pageAfterUpdate(photos, pagerState.currentPage, latest)
                // One snapshot, so nothing ever reads the new pages with the old page or vice versa.
                Snapshot.withMutableSnapshot {
                    photos = latest
                    if (latest.isNotEmpty()) pagerState.requestScrollToPage(page)
                }
            }
    }
}

/**
 * @param latestPhotos the media in the feed, as it is now.
 * @param openedPhotoKey the photo the viewer starts on.
 */
@Composable
internal fun rememberPhotoViewerState(
    latestPhotos: List<MediaUiItem>,
    openedPhotoKey: String
): PhotoViewerState {
    val currentLatestPhotos by rememberUpdatedState(latestPhotos)
    // Saved by key, because the index it was at means nothing in the feed it is restored into.
    val state = rememberSaveable(
        saver = Saver(
            save = { it.activePhotoKey },
            restore = { key -> PhotoViewerState(latestPhotos, key) }
        )
    ) {
        PhotoViewerState(latestPhotos, openedPhotoKey)
    }
    LaunchedEffect(state) {
        state.followFeed { currentLatestPhotos }
    }
    return state
}

/**
 * The page the photo viewer moves to when its pages change from [oldPages] to [newPages] while it
 * is on [oldPage]: the same photo at its new index or, if that photo is gone, its nearest neighbour
 * that is still there, preferring the one after it.
 */
internal fun pageAfterUpdate(
    oldPages: List<MediaUiItem>,
    oldPage: Int,
    newPages: List<MediaUiItem>
): Int {
    if (newPages.isEmpty()) return 0
    val currentKey = oldPages.getOrNull(oldPage)?.key
    // The common case while media is processed: nothing was inserted before the current photo.
    if (currentKey != null && newPages.getOrNull(oldPage)?.key == currentKey) return oldPage

    val newIndexByKey = HashMap<String, Int>(newPages.size)
    newPages.forEachIndexed { index, page -> newIndexByKey[page.key] = index }
    fun newIndexOf(oldIndex: Int): Int? = oldPages.getOrNull(oldIndex)?.let { newIndexByKey[it.key] }

    newIndexOf(oldPage)?.let { return it }
    for (distance in 1..oldPages.size) {
        newIndexOf(oldPage + distance)?.let { return it }
        newIndexOf(oldPage - distance)?.let { return it }
    }
    return oldPage.coerceIn(0, newPages.lastIndex)
}
