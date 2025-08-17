package com.serratocreations.phovo.data.photos.local.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.util.UriSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PhovoImageItem(
    @Serializable(with = UriSerializer::class) override val uri: Uri,
    override val name: String,
    override val dateInFeed: LocalDateTime,
    override val size: Int
) : PhovoItem