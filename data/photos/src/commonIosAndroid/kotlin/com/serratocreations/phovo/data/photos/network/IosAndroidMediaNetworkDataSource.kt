package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.network.model.SyncError
import com.serratocreations.phovo.data.photos.network.model.SyncResult
import com.serratocreations.phovo.data.photos.util.getPlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.source
import io.ktor.client.HttpClient
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.readByteArray

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
        var partIndex = 0

        suspend fun processChunk(byteArray: ByteArray) {
            log.i { "Uploading chunk $partIndex size=${byteArray.size}" }

            val response = syncChunk(
                chunk = byteArray,
                fileName = mediaItemDto.fileName,
                partIndex = partIndex.toString()
            )

            if (response.status.isSuccess()) {
                log.i { "Uploaded chunk $partIndex" }
            } else {
                log.e { "Failed chunk $partIndex: ${response.status}" }
                throw IOException("Failed to upload chunk $partIndex: ${response.status}")
            }
            partIndex++
        }

        return file.source().buffered().use { source ->
            val sink = Buffer()
            while (!source.exhausted()) {
                // Read up to chunkSize bytes, may be smaller at EOF
                val bytesRead = source.readAtMostTo(sink, DEFAULT_CHUNK_SIZE.toLong())
                if (bytesRead <= 0) break  // EOF safety
                // TODO Add retry mechanism
                try {
                    processChunk(sink.readByteArray(bytesRead.toInt()))
                } catch (e: IOException) {
                    return@use SyncError
                }
            }
            return@use completeSuccessfulUpload(mediaItemDto = mediaItemDto)
        }
    }
}