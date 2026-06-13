package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.UploadInitResponse
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
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
import com.serratocreations.phovo.data.photos.mappers.toMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException

abstract class MediaNetworkDataSource(
    private val client: HttpClient,
    logger: PhovoLogger
) {
    companion object {
        // TODO IP should be provided instead of hardcoded and it must come from [ServerConfigRepository]
        //private const val IP = "10.0.0.231:8080"
    }
    private val log = logger.withTag("MediaNetworkDataSource")

    fun allItemsFlow(baseUrl: BaseUrl): Flow<List<MediaItem>> = flow {
        try {
            val response = client.get(baseUrl / ApiEndpoints.GET_ALL_MEDIA_API)
            if (response.status.isSuccess()) {
                val mediaItemDtos = response.body<List<MediaItemDto>>()
                val mediaItems = mediaItemDtos.map { dto ->
                    dto.toMediaItem()
                }
                emit(mediaItems)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            log.e(e) { "Error fetching all items from server" }
            emit(emptyList())
        }
    }

    /**
     * Returns true if a connection to the server is successfully established,
     * otherwise returns false. Connection can fail for a variety of reasons and this API does not
     * currently return a failure reason.
     */
    suspend fun checkServerConnection(baseUrl: BaseUrl): Boolean {
        try {
            val result = client.get(baseUrl / ApiEndpoints.CHECK_ALIVE_API)
            return result.status.isSuccess()
        } catch (_: IOException) {
            return false
        }
    }

    suspend fun syncMedia(
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl
    ): SyncResult {
        log.i { "syncMedia $mediaItemDto" }

        val uploadUrl = baseUrl / ApiEndpoints.Upload.INIT_API
        val initResponse = try {
            client.post(uploadUrl) {
                contentType(ContentType.Application.Json)
                setBody(mediaItemDto)
            }.body<UploadInitResponse>()
        } catch (e: IOException) {
            val errorMessage = "error initializing upload $e"
            log.e { errorMessage }
            return SyncResult.SyncError(errorMessage)
        }

        if (!initResponse.uploadRequired) {
            log.i { "Skipping upload: ${initResponse.message}" }
            return SyncResult.SyncSuccessful
        }

        return chunkedUpload(mediaItemDto, mediaUri, baseUrl)
    }

    protected abstract suspend fun chunkedUpload(
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl
    ): SyncResult

    protected suspend fun syncChunk(
        chunk: ByteReadChannel,
        fileName: String,
        partIndex: String,
        baseUrl: BaseUrl
    ): HttpResponse {
        val chunkUrl = baseUrl / ApiEndpoints.Upload.CHUNK_API
        return client.post(chunkUrl) {
            header("X-File-Name", fileName)
            header("X-Chunk-Index", partIndex)
            setBody(chunk)
        }
    }

    protected suspend fun completeSuccessfulUpload(
        mediaItemDto: MediaItemDto,
        baseUrl: BaseUrl
    ): SyncResult {
        val uploadUrl = baseUrl / ApiEndpoints.Upload.COMPLETE_API
        return try {
            val response = client.post(uploadUrl) {
                contentType(ContentType.Text.Plain)
                setBody(mediaItemDto.assetHash)
            }

            if (response.status.isSuccess()) {
                log.i { "Upload complete for ${mediaItemDto.fileName}" }
                SyncResult.SyncSuccessful
            } else {
                val errorMessage = "Upload completion failed with status: ${response.status}"
                log.e { errorMessage }
                SyncResult.SyncError(errorMessage)
            }
        } catch (e: IOException) {
            val errorMessage = "Upload completion failed: ${e.message}"
            log.e { errorMessage }
            SyncResult.SyncError(errorMessage)
        }
    }
}