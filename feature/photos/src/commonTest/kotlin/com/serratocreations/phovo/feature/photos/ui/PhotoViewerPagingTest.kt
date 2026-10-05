package com.serratocreations.phovo.feature.photos.ui

import coil3.toUri
import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoViewerPagingTest {

    @Test
    fun unchangedBeforeCurrentPage_staysOnPage() {
        val old = pages("a", "b", "c")

        assertEquals(1, pageAfterUpdate(old, 1, pages("a", "b", "c", "d")))
    }

    @Test
    fun itemsInsertedBeforeCurrentPage_followsThePhoto() {
        val old = pages("a", "b", "c")

        assertEquals(3, pageAfterUpdate(old, 1, pages("x", "a", "y", "b", "c")))
    }

    @Test
    fun manyItemsInsertedBeforeCurrentPage_followsThePhoto() {
        // Further than the pager's own key matching looks, which is what made the viewer jump.
        val inserted = (0 until 500).map { "new$it" }
        val old = pages("a", "b", "c")

        assertEquals(501, pageAfterUpdate(old, 1, pages(*(inserted + listOf("a", "b", "c")).toTypedArray())))
    }

    @Test
    fun feedReordered_followsThePhoto() {
        val old = pages("a", "b", "c")

        assertEquals(0, pageAfterUpdate(old, 2, pages("c", "a", "b")))
    }

    @Test
    fun currentPhotoRemoved_movesToTheNextPhoto() {
        val old = pages("a", "b", "c")

        assertEquals(1, pageAfterUpdate(old, 1, pages("a", "c")))
    }

    @Test
    fun currentPhotoAndNextRemoved_movesToThePreviousPhoto() {
        val old = pages("a", "b", "c")

        assertEquals(0, pageAfterUpdate(old, 1, pages("a")))
    }

    @Test
    fun nothingInCommon_staysWithinBounds() {
        val old = pages("a", "b", "c")

        assertEquals(1, pageAfterUpdate(old, 2, pages("x", "y")))
    }

    @Test
    fun newPagesEmpty_returnsFirstPage() {
        assertEquals(0, pageAfterUpdate(pages("a", "b"), 1, emptyList()))
    }

    private fun pages(vararg keys: String) = keys.map { key ->
        val location = DomainAssetLocation.RemoteAssetLocation(
            remoteAssetUri = "https://phovo.test/$key".toUri(),
            assetId = key
        )
        ImagePhotoUiItem(
            sourceAsset = location,
            lowResThumbnail = null,
            thumbnail = location,
            key = key
        )
    }
}
