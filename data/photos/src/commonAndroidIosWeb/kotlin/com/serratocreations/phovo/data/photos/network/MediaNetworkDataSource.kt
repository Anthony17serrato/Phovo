package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.SyncError
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
import com.serratocreations.phovo.data.photos.repository.model.SyncSuccessful
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.IOException

abstract class MediaNetworkDataSource(
    private val client: HttpClient,
    logger: PhovoLogger
) {
    companion object {
        private const val IP = "192.168.1.21:8080"
    }
    private val log = logger.withTag("MediaNetworkDataSource")

    // TODO: Implement network API for getting all items
    fun allItemsFlow(): Flow<List<MediaItem>> = flowOf()

    /**
     * Returns true if a connection to the server is successfully established,
     * otherwise returns false. Connection can fail for a variety of reasons and this API does not
     * currently return a failure reason.
     */
    suspend fun checkServerConnection(): Boolean {
        try {
            val result = client.get("http://$IP/")
            return result.status.isSuccess()
        } catch (_: IOException) {
            return false
        }
    }

    suspend fun syncMedia(
        mediaItemDto: MediaItemDto,
        mediaUri: String
    ): SyncResult {
        log.i { "syncMedia $mediaItemDto" }

        // Step 1: Init
        try {
            client.post("http://$IP/upload/init") {
                contentType(ContentType.Application.Json)
                setBody(mediaItemDto)
            }
        } catch (e: IOException) {
            log.e { "error initializing upload $e" }
            return SyncError
        }

        // Step 2: Chunked upload
        return chunkedUpload(mediaItemDto, mediaUri)
    }

    protected abstract suspend fun chunkedUpload(
        mediaItemDto: MediaItemDto,
        mediaUri: String
    ): SyncResult

    protected suspend fun syncChunk(
        chunk: ByteReadChannel,
        fileName: String,
        partIndex: String
    ): HttpResponse {
        return client.post("http://$IP/upload/chunk") {
            header("X-File-Name", fileName)
            header("X-Chunk-Index", partIndex)
            setBody(chunk)
        }
    }

    protected suspend fun completeSuccessfulUpload(mediaItemDto: MediaItemDto): SyncResult {
        try {
            val updatedItem: MediaItemDto = client.post("http://$IP/upload/complete") {
                contentType(ContentType.Text.Plain)
                setBody(mediaItemDto.localUuid)
            }.body()

            log.i { "Upload complete for ${updatedItem.fileName}" }
            return SyncSuccessful(updatedItem)
        } catch (e: IOException) {
            log.e { "Upload completion failed: ${e.message}" }
            return SyncError
        }
    }
}