package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.getPlatform
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
    // TODO actually repository model should handle correct uri mapping
    val uri = if (getPlatform() == Platform.Desktop) {
        // TODO fix code smell
        remoteUri!!
    } else {
        localUri
    }
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