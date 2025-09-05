package com.serratocreations.phovo.data.photos.local.extensions

import coil3.toUri
import com.serratocreations.phovo.core.database.entities.MediaType
import com.serratocreations.phovo.core.database.entities.PhovoMediaEntity
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds

fun PhovoMediaEntity.toMediaItem(): MediaItem {
    // Combine UTC timestamp with offset to get local wall-clock time for feed
    val dateInFeed = Instant
        .fromEpochMilliseconds(timeStampUtcMs + timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    return when (mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                uri = localUri.toUri(),
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size
            )
        }
        MediaType.Video -> {
            val duration = (videoDurationMs ?: 0L).milliseconds
            MediaVideoItem(
                uri = localUri.toUri(),
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                duration = duration
            )
        }
    }
}
