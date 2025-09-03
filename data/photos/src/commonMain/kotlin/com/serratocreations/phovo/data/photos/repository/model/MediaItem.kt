package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    val uri: Uri
    val fileName: String
    // TODO MediaItem should use ZonedDateTime
    val dateInFeed: LocalDateTime
    val size: Int
}
