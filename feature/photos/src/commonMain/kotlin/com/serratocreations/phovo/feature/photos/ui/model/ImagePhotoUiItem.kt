package com.serratocreations.phovo.feature.photos.ui.model

import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset

data class ImagePhotoUiItem(
    override val sourceAsset: LocalOrRemoteAsset,
    override val lowResThumbnail: LocalOrRemoteAsset?,
    override val thumbnail: LocalOrRemoteAsset,
    override val key: String
) : ThumbnailPhotoUiItem

data class VideoPhotoUiItem(
    override val sourceAsset: LocalOrRemoteAsset,
    override val lowResThumbnail: LocalOrRemoteAsset?,
    val duration: String,
    override val thumbnail: LocalOrRemoteAsset,
    override val key: String
) : ThumbnailPhotoUiItem

sealed interface ThumbnailPhotoUiItem : PhotoUiItem {
    /** Source quality media URI */
    val sourceAsset: LocalOrRemoteAsset
    val lowResThumbnail: LocalOrRemoteAsset?
    val thumbnail: LocalOrRemoteAsset
}