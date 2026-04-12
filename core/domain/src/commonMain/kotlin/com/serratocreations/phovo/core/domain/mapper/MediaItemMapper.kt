package com.serratocreations.phovo.core.domain.mapper

import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem

fun MediaItem.toMediaItemWithThumbnails(
    lowResThumbnailLocation: LocalOrRemoteAsset?,
    highResThumbnailLocation: LocalOrRemoteAsset
): MediaItemWithThumbnails {
    return when(this) {
        is MediaImageItem -> {
            MediaItemWithThumbnails.MediaImageItem(
                assetLocation = assetLocation,
                lowResThumbnailLocation = lowResThumbnailLocation,
                highResThumbnailLocation = highResThumbnailLocation,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                localUuid = uniqueAssetIdentifier
            )
        }
        is MediaVideoItem -> {
            MediaItemWithThumbnails.MediaVideoItem(
                assetLocation = assetLocation,
                lowResThumbnailLocation = lowResThumbnailLocation,
                highResThumbnailLocation = highResThumbnailLocation,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                localUuid = uniqueAssetIdentifier,
                duration = duration
            )
        }
    }
}