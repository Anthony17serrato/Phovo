package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.util.UriSerializer
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MediaImageItem(
    // TODO don't recall why serializable is used, investigate if can be removed
    @Serializable(with = UriSerializer::class)override val uri: Uri,
    override val lowResThumbnail: PlatformFile?,
    @Serializable(with = UriSerializer::class)override val thumbnailUri: Uri = uri,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int,
    override val localUuid: String,
    override val remoteUuid: String?,
) : MediaItem