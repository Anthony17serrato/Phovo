package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import kotlinx.datetime.LocalDateTime

data class MediaImageItem(
    override val uri: Uri,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int,
    override val localUuid: String,
    override val remoteUuid: String?,
) : MediaItem