package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    val localUuid: String
    val remoteUuid: String?
    /** The location where the media can be accessed */
    val uri: Uri

    /**
     * Uri for thumbnail, in the case of photos this Uri is the same as [uri]
     * For videos there may be a dedicated thumbnail for display.
     */
    val thumbnailUri: Uri

    /**
     * If a thumbnail is available this property is populated
     */
    val lowResThumbnail: PlatformFile?
    val fileName: String
    // TODO MediaItem should use ZonedDateTime
    val dateInFeed: LocalDateTime
    // TODO Does size need to be long?
    val size: Int
}
