package com.serratocreations.phovo.data.photos.db.entity

import coil3.Uri
import kotlinx.datetime.LocalDateTime

interface PhovoItem {
    val uri: Uri
    val name: String
    val dateInFeed: LocalDateTime
    val size: Int
}
