package com.serratocreations.phovo.data.photos.network.model

import java.net.URI as DesktopUri
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow
import java.io.File

// TODO Until multi server support is added this implementation is not used
class DesktopNetworkFile(
    override val mediaItem: MediaItem
) : NetworkFile {
    private val file = File(DesktopUri.create(mediaItem.uri.toString()))

    override suspend fun exists(): Boolean = file.exists()

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> {
        TODO("Not yet implemented")
        file.readBytes()
    }
}