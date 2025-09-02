package com.serratocreations.phovo.data.photos.repository.extensions

import com.serratocreations.phovo.core.database.entities.MediaType
import com.serratocreations.phovo.core.database.entities.PhovoMediaEntity
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun MediaItem.toPhovoMediaEntity(): PhovoMediaEntity {
    // TODO MediaItem should use ZonedDateTime
    val instant = dateInFeed.toInstant(TimeZone.UTC)
    val timeStampUtcMs = instant.toEpochMilliseconds()
    val timeOffsetMs = 0L

    val (mediaType, videoDurationMs) = when (this) {
        is MediaImageItem -> MediaType.Image to null
        is MediaVideoItem -> MediaType.Video to this.duration.inWholeMilliseconds
    }

    return PhovoMediaEntity(
        localUri = uri.toString(),
        remoteUri = null,
        remoteThumbnailUri = null,
        name = name,
        timeStampUtcMs = timeStampUtcMs,
        timeOffsetMs = timeOffsetMs,
        size = size,
        mediaType = mediaType,
        videoDurationMs = videoDurationMs,
    )
}