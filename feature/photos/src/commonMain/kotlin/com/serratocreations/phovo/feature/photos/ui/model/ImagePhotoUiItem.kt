package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.local.model.PhovoImageItem
import com.serratocreations.phovo.data.photos.local.model.PhovoItem
import com.serratocreations.phovo.data.photos.local.model.PhovoVideoItem
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

fun PhovoItem.toPhotoUiItem(): PhotoUiItem {
    return when (this) {
        is PhovoImageItem -> {
            ImagePhotoUiItem(uri = uri)
        }
        is PhovoVideoItem -> {
            VideoPhotoUiItem(
                uri = uri,
                duration = duration.toFormattedDurationString()
            )
        }
    }
}