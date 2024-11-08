package com.serratocreations.phovo.feature.photos.data.db.entity

import coil3.Uri

interface PhovoItem {
    val uri: Uri
    val name: String
    val size: Int
}
