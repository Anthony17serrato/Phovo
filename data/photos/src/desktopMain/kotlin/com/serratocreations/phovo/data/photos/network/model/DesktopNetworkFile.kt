package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.model.network.MediaItemDto
import java.net.URI as DesktopUri
import kotlinx.coroutines.flow.Flow
import java.io.File

// TODO Move networkfile to android/ios source sets and delete
class DesktopNetworkFile(
    override val mediaItemDto: MediaItemDto
) : NetworkFile {
    private val file = File(DesktopUri.create(mediaItemDto.localUri))

    override suspend fun exists(): Boolean = file.exists()

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> {
        TODO("Not yet implemented")
        file.readBytes()
    }
}