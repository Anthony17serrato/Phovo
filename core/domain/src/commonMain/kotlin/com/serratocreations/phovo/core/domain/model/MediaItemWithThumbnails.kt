package com.serratocreations.phovo.core.domain.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

sealed interface MediaItemWithThumbnails {
    val localUuid: String
    /** The location where the media can be accessed */
    val assetLocation: DomainAssetLocation

    /**
     * Uri for thumbnail.
     */
    val highResThumbnailLocation: DomainAssetLocation

    /**
     * If a low res thumbnail is available this property is populated
     */
    val lowResThumbnailLocation: DomainAssetLocation?
    val fileName: String
    val dateInFeed: LocalDateTime
    val size: Long

    data class MediaImageItem(
        override val assetLocation: DomainAssetLocation,
        override val lowResThumbnailLocation: DomainAssetLocation?,
        override val highResThumbnailLocation: DomainAssetLocation = assetLocation,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Long,
        override val localUuid: String,
    ) : MediaItemWithThumbnails

    data class MediaVideoItem(
        override val assetLocation: DomainAssetLocation,
        override val lowResThumbnailLocation: DomainAssetLocation?,
        override val highResThumbnailLocation: DomainAssetLocation = assetLocation,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Long,
        override val localUuid: String,
        val duration: Duration,
    ) : MediaItemWithThumbnails
}