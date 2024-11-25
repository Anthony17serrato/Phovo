package com.serratocreations.phovo.feature.photos.data.db.entity

import coil3.Uri
import kotlinx.datetime.LocalDateTime

data class PhovoImageItem(
    override val uri: Uri,
    override val name: String,
    override val dateTaken: LocalDateTime?,
    override val size: Int
) : PhovoItem