package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    val localUuid: String
    val remoteUuid: String?
    /** The location where the media can be accessed */
    val uri: Uri
    val remoteThumbnailUri: Uri?
    val fileName: String
    // TODO MediaItem should use ZonedDateTime
    val dateInFeed: LocalDateTime
    // TODO Does size need to be long?
    val size: Int
}
