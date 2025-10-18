package com.serratocreations.phovo.data.photos.mappers

import coil3.toUri
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.database.entities.MediaItemUriEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithUriEntity
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun MediaItemWithUriEntity.toMediaItem(): MediaItem {
    // Combine UTC timestamp with offset to get local wall-clock time for feed
    val dateInFeed = Instant
        .fromEpochMilliseconds(mediaItemEntity.timeStampUtcMs + mediaItemEntity.timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    return when (mediaItemEntity.mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                localUuid = mediaItemEntity.localUuid,
                remoteUuid = mediaItemEntity.remoteUuid,
                uri = mediaItemUri.uri.toUri(),
                remoteThumbnailUri = mediaItemEntity.remoteThumbnailUri?.toUri(),
                fileName = mediaItemEntity.fileName,
                dateInFeed = dateInFeed,
                size = mediaItemEntity.size
            )
        }
        MediaType.Video -> {
            val duration = (mediaItemEntity.videoDurationMs ?: 0L).milliseconds
            MediaVideoItem(
                localUuid = mediaItemEntity.localUuid,
                remoteUuid = mediaItemEntity.remoteUuid,
                uri = mediaItemUri.uri.toUri(),
                remoteThumbnailUri = mediaItemEntity.remoteThumbnailUri?.toUri(),
                fileName = mediaItemEntity.fileName,
                dateInFeed = dateInFeed,
                size = mediaItemEntity.size,
                duration = duration
            )
        }
    }
}

//fun MediaItemEntity.toMediaItemDto(): MediaItemDto = MediaItemDto(
//    fileName = fileName,
//    localUuid = localUuid,
//    remoteUuid = remoteUuid,
//    remoteThumbnailUri = remoteThumbnailUri,
//    size = size.toLong(),
//    timeStampUtcMs = timeStampUtcMs,
//    timeOffsetMs = timeOffsetMs,
//    mediaType = mediaType,
//    videoDurationMs = videoDurationMs,
//)

fun Flow<List<MediaItemWithUriEntity>>.toMediaItems(): Flow<List<MediaItem>> =
    map { localItems ->
        localItems.map {
            it.toMediaItem()
        }
    }

fun MediaItemWithUriEntity.toMediaItemDto(): MediaItemDto = MediaItemDto(
    fileName = mediaItemEntity.fileName,
    localUuid = mediaItemEntity.localUuid,
    remoteUuid = mediaItemEntity.remoteUuid,
    remoteThumbnailUri = mediaItemEntity.remoteThumbnailUri,
    size = mediaItemEntity.size.toLong(),
    timeStampUtcMs = mediaItemEntity.timeStampUtcMs,
    timeOffsetMs = mediaItemEntity.timeOffsetMs,
    mediaType = mediaItemEntity.mediaType,
    videoDurationMs = mediaItemEntity.videoDurationMs,
)

fun MediaItemDto.toMediaItemEntity(): MediaItemEntity {
    return MediaItemEntity(
        localUuid = localUuid,
        remoteUuid = remoteUuid,
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
fun MediaItem.toMediaItemWithUriEntity(): MediaItemWithUriEntity {
    // TODO MediaItem should use ZonedDateTime
    val instant = dateInFeed.toInstant(TimeZone.UTC)
    val timeStampUtcMs = instant.toEpochMilliseconds()
    val timeOffsetMs = 0L

    val (mediaType, videoDurationMs) = when (this) {
        is MediaImageItem -> MediaType.Image to null
        is MediaVideoItem -> MediaType.Video to this.duration.inWholeMilliseconds
    }

    return MediaItemWithUriEntity(
        mediaItemEntity = MediaItemEntity(
            localUuid = localUuid,
            remoteUuid = remoteUuid,
            remoteThumbnailUri = remoteThumbnailUri?.toString(),
            fileName = fileName,
            timeStampUtcMs = timeStampUtcMs,
            timeOffsetMs = timeOffsetMs,
            size = size,
            mediaType = mediaType,
            videoDurationMs = videoDurationMs,
        ),
        mediaItemUri = MediaItemUriEntity(
            mediaUuid = localUuid,
            uri = uri.toString(),
        )
    )
}

//@OptIn(ExperimentalTime::class)
//fun MediaItemDto.toMediaItem(): MediaItem {
//    // Combine UTC timestamp with offset to get local wall-clock time for feed
//    val dateInFeed = Instant
//        .fromEpochMilliseconds(timeStampUtcMs + timeOffsetMs)
//        .toLocalDateTime(TimeZone.UTC)
//
//    return when (mediaType) {
//        MediaType.Image -> {
//            MediaImageItem(
//                localUuid = localUuid,
//                remoteUuid = remoteUuid,
//                uri = ur.toUri(),
//                remoteUri = remoteUri?.toUri(),
//                remoteThumbnailUri = remoteThumbnailUri?.toUri(),
//                fileName = fileName,
//                dateInFeed = dateInFeed,
//                size = size.toInt()
//            )
//        }
//        MediaType.Video -> {
//            val duration = (videoDurationMs ?: 0L).milliseconds
//            MediaVideoItem(
//                localUuid = localUuid,
//                remoteUuid = remoteUuid,
//                uri = localUri.toUri(),
//                remoteUri = remoteUri?.toUri(),
//                remoteThumbnailUri = remoteThumbnailUri?.toUri(),
//                fileName = fileName,
//                dateInFeed = dateInFeed,
//                size = size.toInt(),
//                duration = duration
//            )
//        }
//    }
//}
