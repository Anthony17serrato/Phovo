package com.serratocreations.phovo.feature.photos.ui.model

import coil3.Uri
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem

data class ImagePhotoUiItem(
    val uri: Uri
) : PhotoUiItem

fun PhovoItem.toImagePhotoUiItem() = ImagePhotoUiItem(uri = uri)