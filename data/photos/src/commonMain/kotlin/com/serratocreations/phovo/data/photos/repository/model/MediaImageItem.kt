package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.util.UriSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MediaImageItem(
    @Serializable(with = UriSerializer::class) override val uri: Uri,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int
) : MediaItem