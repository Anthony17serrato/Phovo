package com.serratocreations.phovo.data.db.entity

import coil3.Uri

data class PhovoImageItem(
    override val uri: Uri,
    override val name: String,
    override val size: Int
) : PhovoItem