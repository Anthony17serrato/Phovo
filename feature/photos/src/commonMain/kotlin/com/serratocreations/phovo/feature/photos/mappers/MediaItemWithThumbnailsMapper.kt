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
                uri = uri,
                lowResThumbnail = lowResThumbnail,
                thumbnail = thumbnailUri,
                key = this.localUuid
            )
        }
        is MediaItemWithThumbnails.MediaVideoItem -> {
            VideoPhotoUiItem(
                uri = uri,
                duration = duration.toFormattedDurationString(),
                lowResThumbnail = lowResThumbnail,
                thumbnail = thumbnailUri,
                key = this.localUuid
            )
        }
    }
}