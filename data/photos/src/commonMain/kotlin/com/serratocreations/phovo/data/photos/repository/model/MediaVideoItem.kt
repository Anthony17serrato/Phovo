package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.util.UriSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class MediaVideoItem(
    // TODO don't recall why serializable is used, investigate if can be removed
    @Serializable(with = UriSerializer::class)override val uri: Uri,
    @Serializable(with = UriSerializer::class)override val remoteThumbnailUri: Uri?,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int,
    override val localUuid: String,
    override val remoteUuid: String?,
    val duration: Duration,
) : MediaItem