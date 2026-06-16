package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.UploadInitResponse
import com.serratocreations.phovo.data.photos.repository.model.NetworkResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException
import kotlin.math.pow
import kotlin.random.Random
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
        baseUrl: BaseUrl
    ): NetworkResult<Unit> = networkResultCallWrapper {
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

sealed interface NetworkCallRetryPolicy {
    /**
     * The number of attempts which will be made after the first failure
     */
    val retryAttempts: Int

    /**
     * Executes the delay/suspension for the current retry attempt.
     * @param attemptIndex The 0-based index of the current retry.
     */
    suspend fun executeDelay(attemptIndex: Int)

    data object NONE : NetworkCallRetryPolicy {
        override val retryAttempts: Int = 0
        override suspend fun executeDelay(attemptIndex: Int) {}
    }

    data class RetryAfterDelay(
        override val retryAttempts: Int = 3,
        val delayDuration: Duration
    ) : NetworkCallRetryPolicy {
        override suspend fun executeDelay(attemptIndex: Int) {
            delay(delayDuration)
        }
    }

    data class RetryAfterLambda(
        override val retryAttempts: Int = 3,
        val lambda: suspend (attemptIndex: Int) -> Unit
    ) : NetworkCallRetryPolicy {
        override suspend fun executeDelay(attemptIndex: Int) {
            lambda(attemptIndex)
        }
    }

    class ExponentialBackoff(
        override val retryAttempts: Int = 3,
        val initialDelay: Duration,
        val maxDelay: Duration,
        val multiplier: Double = 2.0
    ) : NetworkCallRetryPolicy {
        override suspend fun executeDelay(attemptIndex: Int) {
            val factor = multiplier.pow(attemptIndex.toDouble())
            val delayMs = (initialDelay.inWholeMilliseconds * factor).toLong()
            val finalDelayMs = minOf(delayMs, maxDelay.inWholeMilliseconds)

            val jitterFactor = Random.nextDouble(-0.1, 0.1)
            val jitter = (finalDelayMs * jitterFactor).toLong()

            delay((finalDelayMs + jitter).coerceAtLeast(0))
        }
    }
}

suspend fun <T> networkResultCallWrapper(
    retryPolicy: NetworkCallRetryPolicy = NetworkCallRetryPolicy.NONE,
    networkCall: suspend () -> NetworkResult<T>
): NetworkResult<T> {
    suspend fun getResult() = try {
        networkCall()
    } catch (e: Exception) {
        when (e) {
            is IOException -> NetworkResult.NetworkError(message = "$e")
            else -> throw e
        }
    }

    var result: NetworkResult<T> = getResult()
    if (result is NetworkResult.NetworkSuccess) return result

    repeat(retryPolicy.retryAttempts) { attemptIndex ->
        // TODO should not retry if error is network based and call is made inside of a WorkManager
        retryPolicy.executeDelay(attemptIndex)

        result = getResult()
        if (result is NetworkResult.NetworkSuccess) return result
    }

    return result
}

suspend fun networkCallWrapper(
    retryPolicy: NetworkCallRetryPolicy = NetworkCallRetryPolicy.NONE,
    networkCall: suspend () -> HttpResponse
): NetworkResult<HttpResponse> =
    networkResultCallWrapper(
        retryPolicy = retryPolicy
    ) {
        val result = networkCall()
        if (result.status.isSuccess()) {
            NetworkResult.NetworkSuccess(result)
        } else {
            val message = "Network call failed with status: ${result.status}"
            NetworkResult.NetworkError(message = message)
        }
    }