package com.serratocreations.phovo.data.photos.repository.model

import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    val uniqueAssetIdentifier: String
    /** The location where the media can be accessed */
    val assetLocation: LocalOrRemoteAsset
    val fileName: String
    val dateInFeed: LocalDateTime
    val size: Long
}
