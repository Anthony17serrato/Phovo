package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import io.github.vinceglb.filekit.PlatformFile

data class ImagePhotoUiItem(
    override val uri: Uri,
    override val lowResThumbnail: PlatformFile?,
    override val thumbnail: Uri,
    override val key: String
) : ThumbnailPhotoUiItem

data class VideoPhotoUiItem(
    override val uri: Uri,
    override val lowResThumbnail: PlatformFile?,
    val duration: String,
    override val thumbnail: Uri,
    override val key: String
) : ThumbnailPhotoUiItem

sealed interface ThumbnailPhotoUiItem : PhotoUiItem {
    /** Source quality media URI */
    val uri: Uri
    val lowResThumbnail: PlatformFile?
    val thumbnail: Uri
}