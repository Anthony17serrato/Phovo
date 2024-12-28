package com.serratocreations.phovo.data.photos.db.entity

import coil3.Uri
import kotlinx.datetime.LocalDateTime

data class PhovoImageItem(
    override val uri: Uri,
    override val name: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int
) : PhovoItem