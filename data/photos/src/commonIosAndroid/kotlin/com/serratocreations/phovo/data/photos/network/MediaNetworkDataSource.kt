package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkFailure
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.ServerHealth
import com.serratocreations.phovo.core.model.network.UploadInitResponse
import com.serratocreations.phovo.data.photos.mappers.toMediaItem
import com.serratocreations.phovo.data.photos.network.util.networkCallWrapper
import com.serratocreations.phovo.data.photos.network.util.networkResultCallWrapper
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
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
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

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
        val response = networkCallWrapper {
            client.get(baseUrl / ApiEndpoints.GET_ALL_MEDIA_API)
        }
        if (response is NetworkResult.NetworkSuccess) {
            val mediaItemDtos = response.data.body<List<MediaItemDto>>()
            val mediaItems = mediaItemDtos.map { dto ->
                dto.toMediaItem()
            }
            emit(mediaItems)
        } else {
            emit(emptyList())
        }
    }

    /**
     * Probes the server for reachability and identity in one call. Success carries what the server
     * reports about itself; an error carries the classified reason it did not answer.
     */
    suspend fun fetchServerHealth(
        baseUrl: BaseUrl,
        timeout: Duration? = null
    ): NetworkResult<ServerHealth> =
        networkResultCallWrapper {
            val response = client.get(baseUrl / ApiEndpoints.HEALTH_API) {
                timeout {
                    if (timeout != null) {
                        requestTimeoutMillis = timeout.inWholeMilliseconds
                    }
                }
            }
            if (response.status.isSuccess()) {
                NetworkResult.NetworkSuccess(response.body<ServerHealth>())
            } else {
                NetworkResult.NetworkError(
                    message = "Health probe failed with status: ${response.status}",
                    failure = NetworkFailure.HttpStatus
                )
            }
        }

    suspend fun syncMedia(
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl,
        retryPolicy: NetworkCallRetryPolicy,
        onBytesSent: (bytesSent: Long) -> Unit = {}
    ): NetworkResult<Unit> = networkResultCallWrapper(
        retryPolicy = retryPolicy
    ) {
        log.i { "syncMedia $mediaItemDto" }

        val uploadUrl = baseUrl / ApiEndpoints.Upload.INIT_API
        val initResponse = client.post(uploadUrl) {
            contentType(ContentType.Application.Json)
            setBody(mediaItemDto)
        }.body<UploadInitResponse>()

        if (!initResponse.uploadRequired) {
            log.i { "Skipping upload: ${initResponse.message}" }
            return@networkResultCallWrapper NetworkResult.NetworkSuccess(Unit)
        }

        return@networkResultCallWrapper chunkedUpload(mediaItemDto, mediaUri, baseUrl, onBytesSent)
    }

    protected abstract suspend fun chunkedUpload(
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl,
        onBytesSent: (bytesSent: Long) -> Unit
    ): NetworkResult<Unit>

    protected suspend fun syncChunk(
        chunk: ByteReadChannel,
        fileName: String,
        partIndex: String,
        baseUrl: BaseUrl,
        onBytesSent: (bytesSent: Long) -> Unit = {}
    ): HttpResponse {
        val chunkUrl = baseUrl / ApiEndpoints.Upload.CHUNK_API
        return client.post(chunkUrl) {
            header("X-File-Name", fileName)
            header("X-Chunk-Index", partIndex)
            setBody(chunk)
            // Reports as the body is written, so progress moves during a single large upload
            // rather than only when a whole file finishes. Chunking is currently disabled, so
            // without this one big video is one request with no progress at all.
            onUpload { bytesSentTotal, _ -> onBytesSent(bytesSentTotal) }
        }
    }

    protected suspend fun completeSuccessfulUpload(
        mediaItemDto: MediaItemDto,
        baseUrl: BaseUrl
    ): NetworkResult<Unit> {
        val uploadUrl = baseUrl / ApiEndpoints.Upload.COMPLETE_API
        val response = client.post(uploadUrl) {
            contentType(ContentType.Text.Plain)
            setBody(mediaItemDto.assetHash)
        }
        return if (response.status.isSuccess()) {
            log.i { "Upload complete for ${mediaItemDto.fileName}" }
            NetworkResult.NetworkSuccess(Unit)
        } else {
            val errorMessage = "Upload completion failed with status: ${response.status}"
            log.e { errorMessage }
            NetworkResult.NetworkError(errorMessage)
        }
    }
}