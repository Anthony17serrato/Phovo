package com.serratocreations.phovo.data.photos.repository.extensions

import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun MediaItem.toMediaItemEntity(): MediaItemEntity {
    // TODO MediaItem should use ZonedDateTime
    val instant = dateInFeed.toInstant(TimeZone.UTC)
    val timeStampUtcMs = instant.toEpochMilliseconds()
    val timeOffsetMs = 0L

    val (mediaType, videoDurationMs) = when (this) {
        is MediaImageItem -> MediaType.Image to null
        is MediaVideoItem -> MediaType.Video to this.duration.inWholeMilliseconds
    }

    return MediaItemEntity(
        localUuid = localUuid,
        remoteUuid = remoteUuid,
        localUri = localUri.toString(),
        remoteUri = remoteUri?.toString(),
        remoteThumbnailUri = remoteThumbnailUri?.toString(),
        fileName = fileName,
        timeStampUtcMs = timeStampUtcMs,
        timeOffsetMs = timeOffsetMs,
        size = size,
        mediaType = mediaType,
        videoDurationMs = videoDurationMs,
    )
}