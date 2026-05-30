package com.serratocreations.phovo.data.photos.mappers

import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

fun MediaItemDto.toMediaItem(): MediaItem {
    // Combine UTC timestamp with offset to get local wall-clock time for feed
    val dateInFeed = Instant
        .fromEpochMilliseconds(timeStampUtcMs + timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    return when (mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                assetLocation = AssetLocation.RemoteAssetLocation,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                uniqueAssetIdentifier = assetHash,
                isSynced = true
            )
        }
        MediaType.Video -> {
            MediaVideoItem(
                assetLocation = AssetLocation.RemoteAssetLocation,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                uniqueAssetIdentifier = assetHash,
                duration = (videoDurationMs ?: 0L).milliseconds,
                isSynced = true
            )
        }
    }
}