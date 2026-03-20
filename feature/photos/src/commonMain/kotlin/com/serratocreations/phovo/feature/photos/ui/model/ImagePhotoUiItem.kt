package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import coil3.toUri
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.feature.photos.util.toFormattedDurationString
import io.github.vinceglb.filekit.absolutePath

data class ImagePhotoUiItem(
    override val uri: Uri,
    override val lowResThumbnail: Uri?,
    override val thumbnail: Uri,
    override val key: String
) : ThumbnailPhotoUiItem

data class VideoPhotoUiItem(
    override val uri: Uri,
    override val lowResThumbnail: Uri?,
    val duration: String,
    override val thumbnail: Uri,
    override val key: String
) : ThumbnailPhotoUiItem

sealed interface ThumbnailPhotoUiItem : PhotoUiItem {
    /** Source quality media URI */
    val uri: Uri
    val lowResThumbnail: Uri?
    val thumbnail: Uri
}

fun MediaItem.toPhotoUiItem(): PhotoUiItem {
    return when (this) {
        is MediaImageItem -> {
            ImagePhotoUiItem(
                uri = uri,
                lowResThumbnail = lowResThumbnail?.absolutePath()?.toUri(),
                thumbnail = thumbnailUri,
                key = this.localUuid
            )
        }
        is MediaVideoItem -> {
            VideoPhotoUiItem(
                uri = uri,
                duration = duration.toFormattedDurationString(),
                lowResThumbnail = lowResThumbnail?.absolutePath()?.toUri(),
                thumbnail = thumbnailUri,
                key = this.localUuid
            )
        }
    }
}