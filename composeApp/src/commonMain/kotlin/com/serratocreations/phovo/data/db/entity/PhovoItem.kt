package com.serratocreations.phovo.data.db.entity

import coil3.Uri

interface PhovoItem {
    val uri: Uri
    val name: String
    val size: Int
}
