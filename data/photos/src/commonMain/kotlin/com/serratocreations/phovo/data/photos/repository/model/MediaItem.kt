package com.serratocreations.phovo.data.photos.repository.model

import com.serratocreations.phovo.core.model.MediaDimensions
import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    // TODO: rename to asset hash, more easy to understand
    /** A unique identifier for the asset */
    val uniqueAssetIdentifier: String
    /** The location where the media can be accessed */
    val assetLocation: AssetLocation
    val isSynced: Boolean
    val fileName: String
    val dateInFeed: LocalDateTime
    val size: Long
    val dimensions: MediaDimensions
}
