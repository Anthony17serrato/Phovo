package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.feature.photos.util.toFormattedDurationString

data class ImagePhotoUiItem(
    override val uri: Uri,
    override val thumbnail: Uri
) : ThumbnailPhotoUiItem

data class VideoPhotoUiItem(
    override val uri: Uri,
    val duration: String,
    override val thumbnail: Uri
) : ThumbnailPhotoUiItem

sealed interface ThumbnailPhotoUiItem : PhotoUiItem {
    val uri: Uri
    val thumbnail: Uri
}

fun MediaItem.toPhotoUiItem(): PhotoUiItem {
    return when (this) {
        is MediaImageItem -> {
            ImagePhotoUiItem(uri = uri, thumbnail = thumbnailUri)
        }
        is MediaVideoItem -> {
            VideoPhotoUiItem(
                uri = uri,
                duration = duration.toFormattedDurationString(),
                thumbnail = thumbnailUri
            )
        }
    }
}