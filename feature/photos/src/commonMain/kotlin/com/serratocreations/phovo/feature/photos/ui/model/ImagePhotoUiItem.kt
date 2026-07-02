package com.serratocreations.phovo.feature.photos.ui.model

import com.serratocreations.phovo.core.domain.model.DomainAssetLocation

data class ImagePhotoUiItem(
    override val sourceAsset: DomainAssetLocation,
    override val lowResThumbnail: DomainAssetLocation?,
    override val thumbnail: DomainAssetLocation,
    override val key: String
) : MediaUiItem

data class VideoPhotoUiItem(
    override val sourceAsset: DomainAssetLocation,
    override val lowResThumbnail: DomainAssetLocation?,
    val duration: String,
    override val thumbnail: DomainAssetLocation,
    override val key: String
) : MediaUiItem

sealed interface MediaUiItem : PhotoUiItem {
    /** Source quality media URI */
    val sourceAsset: DomainAssetLocation
    val lowResThumbnail: DomainAssetLocation?
    val thumbnail: DomainAssetLocation
}