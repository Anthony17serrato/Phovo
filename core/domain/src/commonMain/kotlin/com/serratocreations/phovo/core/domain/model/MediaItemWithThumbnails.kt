package com.serratocreations.phovo.core.domain.model

import coil3.Uri
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

sealed interface MediaItemWithThumbnails {
    val localUuid: String
    val remoteUuid: String?
    /** The location where the media can be accessed */
    val uri: Uri

    /**
     * Uri for thumbnail.
     */
    val thumbnailUri: Uri

    /**
     * If a thumbnail is available this property is populated
     */
    val lowResThumbnail: PlatformFile?
    val fileName: String
    val dateInFeed: LocalDateTime
    // TODO Does size need to be long?
    val size: Int

    data class MediaImageItem(
        override val uri: Uri,
        override val lowResThumbnail: PlatformFile?,
        override val thumbnailUri: Uri = uri,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Int,
        override val localUuid: String,
        override val remoteUuid: String?,
    ) : MediaItemWithThumbnails

    data class MediaVideoItem(
        override val uri: Uri,
        override val lowResThumbnail: PlatformFile?,
        override val thumbnailUri: Uri = uri,
        override val fileName: String,
        override val dateInFeed: LocalDateTime,
        override val size: Int,
        override val localUuid: String,
        override val remoteUuid: String?,
        val duration: Duration,
    ) : MediaItemWithThumbnails
}