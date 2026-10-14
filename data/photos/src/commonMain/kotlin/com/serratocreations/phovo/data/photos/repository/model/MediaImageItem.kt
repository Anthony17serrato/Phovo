package com.serratocreations.phovo.data.photos.repository.model

import com.serratocreations.phovo.core.model.MediaDimensions
import kotlinx.datetime.LocalDateTime

data class MediaImageItem(
    override val assetLocation: AssetLocation,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Long,
    override val uniqueAssetIdentifier: String,
    override val isSynced: Boolean,
    override val dimensions: MediaDimensions
) : MediaItem