package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.util.UriSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class MediaVideoItem(
    @Serializable(with = UriSerializer::class) override val uri: Uri,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int,
    val duration: Duration
) : MediaItem