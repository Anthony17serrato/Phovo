package com.serratocreations.phovo.data.photos.mappers

import com.serratocreations.phovo.core.database.entities.MediaItemMetadataEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun MediaItemWithMetadata.toMediaItem(): MediaItem {
    // Combine UTC timestamp with offset to get local wall-clock time for feed
    val dateInFeed = Instant
        .fromEpochMilliseconds(mediaItemMetadataEntity.timeStampUtcMs + mediaItemMetadataEntity.timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    val assetLocation = localLocation?.let { locationNotNull ->
        AssetLocation.LocalAssetLocation(
            localAssetLocation = PlatformFile(locationNotNull.localUri)
        )
    } ?: run {
        AssetLocation.RemoteAssetLocation
    }

    return when (mediaItemMetadataEntity.mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                uniqueAssetIdentifier = mediaItemMetadataEntity.assetHash,
                isSynced = mediaItemMetadataEntity.isSynced,
                assetLocation = assetLocation,
                fileName = mediaItemMetadataEntity.fileName,
                dateInFeed = dateInFeed,
                size = mediaItemMetadataEntity.size
            )
        }
        MediaType.Video -> {
            val duration = (mediaItemMetadataEntity.videoDurationMs ?: 0L).milliseconds
            MediaVideoItem(
                uniqueAssetIdentifier = mediaItemMetadataEntity.assetHash,
                isSynced = mediaItemMetadataEntity.isSynced,
                assetLocation = assetLocation,
                fileName = mediaItemMetadataEntity.fileName,
                dateInFeed = dateInFeed,
                size = mediaItemMetadataEntity.size,
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

fun Flow<List<MediaItemWithMetadata>>.toMediaItems(): Flow<List<MediaItem>> =
    map { localItems ->
        localItems.map {
            it.toMediaItem()
        }
    }

fun MediaItemWithMetadata.toMediaItemDto(): MediaItemDto =
    this.mediaItemMetadataEntity.toMediaItemDto()

fun MediaItemMetadataEntity.toMediaItemDto(): MediaItemDto = MediaItemDto(
    fileName = fileName,
    assetHash = assetHash,
    size = size,
    timeStampUtcMs = timeStampUtcMs,
    timeOffsetMs = timeOffsetMs,
    mediaType = mediaType,
    videoDurationMs = videoDurationMs,
)

fun MediaItemDto.toMediaItemEntity(isSynced: Boolean): MediaItemMetadataEntity {
    return MediaItemMetadataEntity(
        assetHash = assetHash,
        isSynced = isSynced,
        fileName = fileName,
        timeStampUtcMs = timeStampUtcMs,
        timeOffsetMs = timeOffsetMs,
        size = size,
        mediaType = mediaType,
        videoDurationMs = videoDurationMs
    )
}

@OptIn(ExperimentalTime::class)
fun MediaItem.toMediaItemWithMetadataEntity(): MediaItemWithMetadata {
    // TODO MediaItem should use ZonedDateTime
    val instant = dateInFeed.toInstant(TimeZone.UTC)
    val timeStampUtcMs = instant.toEpochMilliseconds()
    val timeOffsetMs = 0L

    val (mediaType, videoDurationMs) = when (this) {
        is MediaImageItem -> MediaType.Image to null
        is MediaVideoItem -> MediaType.Video to this.duration.inWholeMilliseconds
    }

    return MediaItemWithMetadata(
        mediaItemMetadataEntity = MediaItemMetadataEntity(
            assetHash = uniqueAssetIdentifier,
            isSynced = isSynced,
            fileName = fileName,
            timeStampUtcMs = timeStampUtcMs,
            timeOffsetMs = timeOffsetMs,
            size = size,
            mediaType = mediaType,
            videoDurationMs = videoDurationMs,
        ),
        localLocation = when(val location = assetLocation) {
            is AssetLocation.LocalAssetLocation -> {
                LocalMediaEntity(
                    assetHash = uniqueAssetIdentifier,
                    localUri = location.localAssetLocation.absolutePath(),
                    isPartial = false
                )
            }
            AssetLocation.RemoteAssetLocation -> {
                null
            }
        }
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
