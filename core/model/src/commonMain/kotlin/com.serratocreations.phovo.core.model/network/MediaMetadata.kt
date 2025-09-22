package com.serratocreations.phovo.core.model.network

import com.serratocreations.phovo.core.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class MediaMetadata(
    val fileName: String,
    val size: Long,
    val mediaType: MediaType
)