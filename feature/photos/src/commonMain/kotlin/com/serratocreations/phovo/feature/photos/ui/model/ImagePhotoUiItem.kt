package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import coil3.toUri
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
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

fun MediaItemWithThumbnails.toPhotoUiItem(): PhotoUiItem {
    return when (this) {
        is MediaItemWithThumbnails.MediaImageItem -> {
            ImagePhotoUiItem(
                uri = uri,
                // TODO Add filekit Coil integration
                lowResThumbnail = lowResThumbnail?.absolutePath()?.toUri(),
                thumbnail = thumbnailUri,
                key = this.localUuid
            )
        }
        is MediaItemWithThumbnails.MediaVideoItem -> {
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