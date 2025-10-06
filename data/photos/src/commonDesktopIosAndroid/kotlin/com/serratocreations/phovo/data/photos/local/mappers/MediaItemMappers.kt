package com.serratocreations.phovo.data.photos.local.mappers

import coil3.toUri
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun MediaItemEntity.toMediaItem(): MediaItem {
    // Combine UTC timestamp with offset to get local wall-clock time for feed
    val dateInFeed = Instant
        .fromEpochMilliseconds(timeStampUtcMs + timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    return when (mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                localUuid = localUuid,
                remoteUuid = remoteUuid,
                localUri = localUri.toUri(),
                remoteUri = remoteUri?.toUri(),
                remoteThumbnailUri = remoteThumbnailUri?.toUri(),
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size
            )
        }
        MediaType.Video -> {
            val duration = (videoDurationMs ?: 0L).milliseconds
            MediaVideoItem(
                localUuid = localUuid,
                remoteUuid = remoteUuid,
                localUri = localUri.toUri(),
                remoteUri = remoteUri?.toUri(),
                remoteThumbnailUri = remoteThumbnailUri?.toUri(),
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                duration = duration
            )
        }
    }
}

fun MediaItemEntity.toMediaItemDto(): MediaItemDto = MediaItemDto(
    fileName = fileName,
    localUuid = localUuid,
    remoteUuid = remoteUuid,
    localUri = localUri,
    remoteUri = remoteUri,
    remoteThumbnailUri = remoteThumbnailUri,
    size = size.toLong(),
    timeStampUtcMs = timeStampUtcMs,
    timeOffsetMs = timeOffsetMs,
    mediaType = mediaType,
    videoDurationMs = videoDurationMs,
)

fun MediaItemDto.toMediaItemEntity(): MediaItemEntity {
    return MediaItemEntity(
        localUuid = localUuid,
        remoteUuid = remoteUuid,
        localUri = localUri,
        remoteUri = remoteUri,
        remoteThumbnailUri = remoteThumbnailUri,
        fileName = fileName,
        timeStampUtcMs = timeStampUtcMs,
        timeOffsetMs = timeOffsetMs,
        size = size.toInt(),
        mediaType = mediaType,
        videoDurationMs = videoDurationMs
    )
}

@OptIn(ExperimentalTime::class)
fun MediaItemDto.toMediaItem(): MediaItem {
    // Combine UTC timestamp with offset to get local wall-clock time for feed
    val dateInFeed = Instant
        .fromEpochMilliseconds(timeStampUtcMs + timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    return when (mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                localUuid = localUuid,
                remoteUuid = remoteUuid,
                localUri = localUri.toUri(),
                remoteUri = remoteUri?.toUri(),
                remoteThumbnailUri = remoteThumbnailUri?.toUri(),
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size.toInt()
            )
        }
        MediaType.Video -> {
            val duration = (videoDurationMs ?: 0L).milliseconds
            MediaVideoItem(
                localUuid = localUuid,
                remoteUuid = remoteUuid,
                localUri = localUri.toUri(),
                remoteUri = remoteUri?.toUri(),
                remoteThumbnailUri = remoteThumbnailUri?.toUri(),
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size.toInt(),
                duration = duration
            )
        }
    }
}
