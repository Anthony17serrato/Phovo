package com.serratocreations.phovo.data.photos.network.model

import java.net.URI as DesktopUri
import coil3.Uri
import java.io.File

// TODO Until multi server support is added this implementation is not used
class DesktopNetworkFile(
    override val uri: Uri
) : NetworkFile {
    private val file = File(DesktopUri.create(uri.toString()))

    override suspend fun exists(): Boolean = file.exists()

    override suspend fun readBytes() = file.readBytes()
}