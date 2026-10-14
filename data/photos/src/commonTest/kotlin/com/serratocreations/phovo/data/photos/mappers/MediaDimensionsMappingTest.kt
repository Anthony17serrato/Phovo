package com.serratocreations.phovo.data.photos.mappers

import com.serratocreations.phovo.core.model.MediaDimensions
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class MediaDimensionsMappingTest {

    @Test
    fun dto_mapsDimensionsToTheItem() {
        assertEquals(MediaDimensions(1080, 1920), dto(width = 1080, height = 1920).toMediaItem().dimensions)
    }

    @Test
    fun dto_roundTripsDimensionsThroughTheEntity() {
        val entity = dto(width = 1080, height = 1920).toMediaItemEntity(isSynced = true)

        assertEquals(1080, entity.toMediaItemDto().displayWidth)
        assertEquals(1920, entity.toMediaItemDto().displayHeight)
    }

    @Test
    fun mediaItem_roundTripsDimensionsThroughTheEntity() {
        val item = MediaVideoItem(
            assetLocation = AssetLocation.RemoteAssetLocation,
            fileName = "clip.mp4",
            dateInFeed = LocalDateTime(2024, 6, 1, 12, 0),
            size = 10,
            uniqueAssetIdentifier = "hash",
            duration = 1.seconds,
            isSynced = true,
            dimensions = MediaDimensions(1080, 1920)
        )

        assertEquals(MediaDimensions(1080, 1920), item.toMediaItemWithMetadataEntity().toMediaItem().dimensions)
    }

    private fun dto(width: Int, height: Int) = MediaItemDto(
        fileName = "clip.mp4",
        assetHash = "hash",
        size = 10,
        timeStampUtcMs = 0,
        timeOffsetMs = 0,
        mediaType = MediaType.Video,
        videoDurationMs = 1000,
        displayWidth = width,
        displayHeight = height
    )
}
