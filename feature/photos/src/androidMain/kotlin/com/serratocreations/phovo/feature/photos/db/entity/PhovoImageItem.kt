package com.serratocreations.phovo.feature.photos.db.entity

import coil3.Uri
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem

data class PhovoImageItem(
    override val uri: Uri,
    override val name: String,
    override val size: Int
) : PhovoItem