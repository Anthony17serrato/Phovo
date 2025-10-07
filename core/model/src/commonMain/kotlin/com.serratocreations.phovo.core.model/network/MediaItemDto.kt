package com.serratocreations.phovo.core.model.network

import com.serratocreations.phovo.core.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class MediaItemDto(
    val fileName: String,
    val localUuid: String,
    val remoteUuid: String?,
    // TODO remote thumbnail is just an endpoint request with uuid, remove from dto
    val remoteThumbnailUri: String?,
    val size: Long,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)
