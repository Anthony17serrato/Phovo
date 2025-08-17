package com.serratocreations.phovo.data.photos.local.model

import coil3.Uri
import kotlinx.datetime.LocalDateTime

sealed interface PhovoItem {
    val uri: Uri
    val name: String
    val dateInFeed: LocalDateTime
    val size: Int
}
