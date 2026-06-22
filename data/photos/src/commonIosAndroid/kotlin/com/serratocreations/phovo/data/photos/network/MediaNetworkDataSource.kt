package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkCallRetryPolicy
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.core.model.network.UploadInitResponse
import com.serratocreations.phovo.data.photos.mappers.toMediaItem
import com.serratocreations.phovo.data.photos.network.util.networkCallWrapper
import com.serratocreations.phovo.data.photos.network.util.networkResultCallWrapper
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
import kotlinx.coroutines.flow.flow

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
     * Returns true if a connection to the server is successfully established,
     * otherwise returns false. Connection can fail for a variety of reasons and this API does not
     * currently return a failure reason.
     */
    suspend fun checkServerConnection(baseUrl: BaseUrl): Boolean {
        val result = networkCallWrapper {
            client.get(baseUrl.value)
        }
        return result is NetworkResult.NetworkSuccess
    }

    suspend fun syncMedia(
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl,
        retryPolicy: NetworkCallRetryPolicy
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

        return@networkResultCallWrapper chunkedUpload(mediaItemDto, mediaUri, baseUrl)
    }

    protected abstract suspend fun chunkedUpload(
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl
    ): NetworkResult<Unit>

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