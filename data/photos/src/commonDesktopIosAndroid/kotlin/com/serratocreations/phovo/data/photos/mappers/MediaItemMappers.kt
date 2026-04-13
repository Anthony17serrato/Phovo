package com.serratocreations.phovo.data.photos.mappers

import coil3.toUri
import com.serratocreations.phovo.core.database.entities.AssetLocation
import com.serratocreations.phovo.core.database.entities.MediaItemMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemLocationEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
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
        .fromEpochMilliseconds(mediaItemMetadata.timeStampUtcMs + mediaItemMetadata.timeOffsetMs)
        .toLocalDateTime(TimeZone.UTC)

    // TODO for now we assume all db media items are stored locally
    val assetLocation = when(mediaItemLocation.assetLocation) {
        AssetLocation.Local -> {
            LocalOrRemoteAsset.LocalAsset(
                localAssetLocation = PlatformFile(mediaItemLocation.uri),
                isAlsoAvailableRemotely = false
            )
        }
        AssetLocation.LocalAndRemote -> {
            LocalOrRemoteAsset.LocalAsset(
                localAssetLocation = PlatformFile(mediaItemLocation.uri),
                isAlsoAvailableRemotely = true
            )
        }
        AssetLocation.Remote -> {
            LocalOrRemoteAsset.RemoteAsset(
                remoteAssetUri = mediaItemLocation.uri.toUri()
            )
        }
    }
    return when (mediaItemMetadata.mediaType) {
        MediaType.Image -> {
            MediaImageItem(
                uniqueAssetIdentifier = mediaItemMetadata.metadataAssetHash,
                assetLocation = assetLocation,
                fileName = mediaItemMetadata.fileName,
                dateInFeed = dateInFeed,
                size = mediaItemMetadata.size
            )
        }
        MediaType.Video -> {
            val duration = (mediaItemMetadata.videoDurationMs ?: 0L).milliseconds
            MediaVideoItem(
                uniqueAssetIdentifier = mediaItemMetadata.metadataAssetHash,
                assetLocation = assetLocation,
                fileName = mediaItemMetadata.fileName,
                dateInFeed = dateInFeed,
                size = mediaItemMetadata.size,
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

fun MediaItemWithMetadata.toMediaItemDto(): MediaItemDto = MediaItemDto(
    fileName = mediaItemMetadata.fileName,
    assetHash = mediaItemMetadata.metadataAssetHash,
    size = mediaItemMetadata.size,
    timeStampUtcMs = mediaItemMetadata.timeStampUtcMs,
    timeOffsetMs = mediaItemMetadata.timeOffsetMs,
    mediaType = mediaItemMetadata.mediaType,
    videoDurationMs = mediaItemMetadata.videoDurationMs,
)

fun MediaItemDto.toMediaItemEntity(): MediaItemMetadata {
    return MediaItemMetadata(
        metadataAssetHash = assetHash,
        fileName = fileName,
        timeStampUtcMs = timeStampUtcMs,
        timeOffsetMs = timeOffsetMs,
        size = size,
        mediaType = mediaType,
        videoDurationMs = videoDurationMs
    )
}

@OptIn(ExperimentalTime::class)
fun MediaItem.toMediaItemWithUriEntity(): MediaItemWithMetadata {
    // TODO MediaItem should use ZonedDateTime
    val instant = dateInFeed.toInstant(TimeZone.UTC)
    val timeStampUtcMs = instant.toEpochMilliseconds()
    val timeOffsetMs = 0L

    val (mediaType, videoDurationMs) = when (this) {
        is MediaImageItem -> MediaType.Image to null
        is MediaVideoItem -> MediaType.Video to this.duration.inWholeMilliseconds
    }

    return MediaItemWithMetadata(
        mediaItemMetadata = MediaItemMetadata(
            metadataAssetHash = uniqueAssetIdentifier,
            fileName = fileName,
            timeStampUtcMs = timeStampUtcMs,
            timeOffsetMs = timeOffsetMs,
            size = size,
            mediaType = mediaType,
            videoDurationMs = videoDurationMs,
        ),
        mediaItemLocation = MediaItemLocationEntity(
            assetHash = uniqueAssetIdentifier,
            assetLocation = when(val location = assetLocation) {
                is LocalOrRemoteAsset.LocalAsset -> {
                    if (location.isAlsoAvailableRemotely) {
                        AssetLocation.LocalAndRemote
                    } else {
                        AssetLocation.Local
                    }
                }
                is LocalOrRemoteAsset.RemoteAsset -> {
                    AssetLocation.Remote
                }
            },
            uri = when(val location = assetLocation) {
                is LocalOrRemoteAsset.LocalAsset -> {
                    location.localAssetLocation.absolutePath()
                }
                is LocalOrRemoteAsset.RemoteAsset -> {
                    location.remoteAssetUri.toString()
                }
            }
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
