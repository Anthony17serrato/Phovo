package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.feature.photos.util.toFormattedDurationString

data class ImagePhotoUiItem(
    override val uri: Uri
) : UriPhotoUiItem

data class VideoPhotoUiItem(
    override val uri: Uri,
    val duration: String
) : UriPhotoUiItem

sealed interface UriPhotoUiItem : PhotoUiItem {
    val uri: Uri
}

fun MediaItem.toPhotoUiItem(): PhotoUiItem {
    return when (this) {
        is MediaImageItem -> {
            ImagePhotoUiItem(uri = uri)
        }
        is MediaVideoItem -> {
            VideoPhotoUiItem(
                uri = uri,
                duration = duration.toFormattedDurationString()
            )
        }
    }
}