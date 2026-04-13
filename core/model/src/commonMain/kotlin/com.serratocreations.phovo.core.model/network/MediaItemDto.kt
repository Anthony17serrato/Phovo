package com.serratocreations.phovo.core.model.network

import com.serratocreations.phovo.core.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class MediaItemDto(
    val fileName: String,
    val assetHash: String,
    val size: Long,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)
