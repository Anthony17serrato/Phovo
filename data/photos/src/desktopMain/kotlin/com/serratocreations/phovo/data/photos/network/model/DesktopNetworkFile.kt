package com.serratocreations.phovo.data.photos.network.model

import java.net.URI as DesktopUri
import coil3.Uri
import java.io.File

class DesktopNetworkFile(
    override val uri: Uri,
    override val fileName: String
) : NetworkFile {
    private val file = File(DesktopUri.create(uri.toString()))

    override fun exists(): Boolean = file.exists()

    override fun readBytes() = file.readBytes()
}