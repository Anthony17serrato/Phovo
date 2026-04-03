package com.serratocreations.phovo.data.photos.repository.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

data class MediaVideoItem(
    override val assetLocation: LocalOrRemoteAsset,
    override val fileName: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int,
    override val localUuid: String,
    override val remoteUuid: String?,
    val duration: Duration,
) : MediaItem