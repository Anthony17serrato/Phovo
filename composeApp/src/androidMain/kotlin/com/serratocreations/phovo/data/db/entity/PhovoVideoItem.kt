package com.serratocreations.phovo.data.db.entity

import android.net.Uri

data class PhovoVideoItem(
    val uri: Uri,
    override val name: String,
    override val duration: Int,
    override val size: Int
) : PhovoItem