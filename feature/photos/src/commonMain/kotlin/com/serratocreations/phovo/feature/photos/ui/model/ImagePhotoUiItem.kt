package com.serratocreations.phovo.feature.photos.ui.model

import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import kotlinx.datetime.LocalDateTime

data class ImagePhotoUiItem(
    override val sourceAsset: DomainAssetLocation,
    override val lowResThumbnail: DomainAssetLocation?,
    override val thumbnail: DomainAssetLocation,
    override val key: String,
    override val aspectRatio: Float,
    override val dateInFeed: LocalDateTime
) : MediaUiItem

data class VideoPhotoUiItem(
    override val sourceAsset: DomainAssetLocation,
    override val lowResThumbnail: DomainAssetLocation?,
    val duration: String,
    override val thumbnail: DomainAssetLocation,
    override val key: String,
    override val aspectRatio: Float,
    override val dateInFeed: LocalDateTime
) : MediaUiItem

sealed interface MediaUiItem : PhotoUiItem {
    /** Source quality media URI */
    val sourceAsset: DomainAssetLocation
    val lowResThumbnail: DomainAssetLocation?
    val thumbnail: DomainAssetLocation
    /** Width over height as displayed. */
    val aspectRatio: Float
    /** When the media was taken, in the time zone it is shown in. */
    val dateInFeed: LocalDateTime
}