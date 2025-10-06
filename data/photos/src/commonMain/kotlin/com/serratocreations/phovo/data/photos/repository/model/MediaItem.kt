package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    val localUuid: String
    val remoteUuid: String?
    // TODO Need better URI wrappers since this one requires dependency on UI library
    val localUri: Uri
    val remoteUri: Uri?
    val remoteThumbnailUri: Uri?
    val fileName: String
    // TODO MediaItem should use ZonedDateTime
    val dateInFeed: LocalDateTime
    // TODO Does size need to be long?
    val size: Int
}
