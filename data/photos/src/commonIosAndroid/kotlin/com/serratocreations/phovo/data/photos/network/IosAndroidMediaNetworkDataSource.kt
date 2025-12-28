package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.SyncError
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
import com.serratocreations.phovo.data.photos.util.getPlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.source
import io.ktor.client.HttpClient
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.io.buffered

class IosAndroidMediaNetworkDataSource(
    client: HttpClient,
    logger: PhovoLogger,
    val ioDispatcher: CoroutineDispatcher
): MediaNetworkDataSource(client, logger) {
    private val log = logger.withTag("IosAndroidMediaNetworkDataSource")

    override suspend fun chunkedUpload(mediaItemDto: MediaItemDto, mediaUri: String): SyncResult {
        val file = mediaItemDto.mediaType.getPlatformFile(mediaUri, ioDispatcher) ?: return SyncError
        if (!file.exists()) {
            log.e { "File not found at $mediaUri" }
            return SyncError
        }

        // TODO chunking has been disabled, for photographs it is mostly not needed, in the future
        //  chunking should be used to allow videos to resume syncing if disruptions occur
        val byteReadChannel = ByteReadChannel(file.source().buffered())
        val response = syncChunk(
            chunk = byteReadChannel,
            fileName = mediaItemDto.fileName,
            partIndex = "1"
        )

        return if (response.status.isSuccess()) {
            log.i { "Uploaded media ${mediaItemDto.localUuid}" }
            completeSuccessfulUpload(mediaItemDto = mediaItemDto)
        } else {
            log.e { "Failed upload ${mediaItemDto.localUuid}: ${response.status}" }
            SyncError
        }
    }
}