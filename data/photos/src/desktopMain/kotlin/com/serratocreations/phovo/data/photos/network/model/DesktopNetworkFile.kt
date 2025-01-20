package com.serratocreations.phovo.data.photos.network.model

import java.net.URI as DesktopUri
import coil3.Uri
import java.io.File

class DesktopNetworkFile(
    override val uri: Uri,
    private val fileName: String
) : NetworkFile {
    private val file = File(DesktopUri.create(uri.toString()))

    override suspend fun fileName(): String = fileName

    override suspend fun exists(): Boolean = file.exists()

    override suspend fun readBytes() = file.readBytes()
}