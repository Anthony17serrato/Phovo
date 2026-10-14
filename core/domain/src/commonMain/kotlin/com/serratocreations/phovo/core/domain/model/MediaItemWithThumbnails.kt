package com.serratocreations.phovo.core.domain.model

import com.serratocreations.phovo.core.model.MediaDimensions
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
    /** The size the media is displayed at. */
    val dimensions: MediaDimensions

    data class MediaImageItem(
        override val assetLocation: DomainAssetLocation,
        override val lowResThumbnailLocation: DomainAssetLocation?,
        override val highResThumbnailLocation: DomainAssetLocation = assetLocation,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Long,
        override val localUuid: String,
        override val dimensions: MediaDimensions,
    ) : MediaItemWithThumbnails

    data class MediaVideoItem(
        override val assetLocation: DomainAssetLocation,
        override val lowResThumbnailLocation: DomainAssetLocation?,
        override val highResThumbnailLocation: DomainAssetLocation = assetLocation,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Long,
        override val localUuid: String,
        override val dimensions: MediaDimensions,
        val duration: Duration,
    ) : MediaItemWithThumbnails
}