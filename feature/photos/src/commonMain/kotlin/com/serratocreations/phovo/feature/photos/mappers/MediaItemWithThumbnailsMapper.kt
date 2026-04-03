package com.serratocreations.phovo.feature.photos.mappers

import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.feature.photos.ui.model.ImagePhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.util.toFormattedDurationString

fun MediaItemWithThumbnails.toPhotoUiItem(): PhotoUiItem {
    return when (this) {
        is MediaItemWithThumbnails.MediaImageItem -> {
            ImagePhotoUiItem(
                sourceAsset = assetLocation,
                lowResThumbnail = lowResThumbnailLocation,
                thumbnail = highResThumbnailLocation,
                key = this.localUuid
            )
        }
        is MediaItemWithThumbnails.MediaVideoItem -> {
            VideoPhotoUiItem(
                sourceAsset = assetLocation,
                duration = duration.toFormattedDurationString(),
                lowResThumbnail = lowResThumbnailLocation,
                thumbnail = highResThumbnailLocation,
                key = this.localUuid
            )
        }
    }
}