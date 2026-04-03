package com.serratocreations.phovo.core.domain.model

import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

sealed interface MediaItemWithThumbnails {
    val localUuid: String
    val remoteUuid: String?
    /** The location where the media can be accessed */
    val assetLocation: LocalOrRemoteAsset

    /**
     * Uri for thumbnail.
     */
    val highResThumbnailLocation: LocalOrRemoteAsset

    /**
     * If a low res thumbnail is available this property is populated
     */
    val lowResThumbnailLocation: LocalOrRemoteAsset?
    val fileName: String
    val dateInFeed: LocalDateTime
    // TODO Does size need to be long?
    val size: Int

    data class MediaImageItem(
        override val assetLocation: LocalOrRemoteAsset,
        override val lowResThumbnailLocation: LocalOrRemoteAsset?,
        override val highResThumbnailLocation: LocalOrRemoteAsset = assetLocation,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Int,
        override val localUuid: String,
        override val remoteUuid: String?,
    ) : MediaItemWithThumbnails

    data class MediaVideoItem(
        override val assetLocation: LocalOrRemoteAsset,
        override val lowResThumbnailLocation: LocalOrRemoteAsset?,
        override val highResThumbnailLocation: LocalOrRemoteAsset = assetLocation,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Int,
        override val localUuid: String,
        override val remoteUuid: String?,
        val duration: Duration,
    ) : MediaItemWithThumbnails
}