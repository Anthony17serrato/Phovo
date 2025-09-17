package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.network.model.getNetworkFile
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.seconds

class MediaNetworkDataSource(
    private val client: HttpClient,
    logger: PhovoLogger
) {
    private val log = logger.withTag("PhotosNetworkDataSource")

    // TODO: Implement network API for getting all items
    fun allItemsFlow(): Flow<List<MediaItem>> = flowOf()

    suspend fun syncMedia(mediaItem: MediaItem) = coroutineScope {
        log.i { "syncImage $mediaItem" }
        val file = getNetworkFile(mediaItem.uri)

        if (!file.exists()) {
            log.e { "File not found at ${mediaItem.uri}" }
            return@coroutineScope
        }
        var partIndex = 0
        file.readInChunks().onEach { fileChunk ->
            log.i { "Uploading chunk $partIndex of size ${fileChunk.size}" }

            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "http://10.0.0.183:8080/upload",
                formData = formData {
                    append("file", fileChunk, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${mediaItem.fileName}")
                        append("X-Chunk-Index", partIndex.toString())
                        append("X-Chunk-Size", fileChunk.size.toString())
                    })
                }
            )

            if (response.status.value in 200..299) {
                log.i { "Chunk $partIndex uploaded successfully" }
            } else {
                log.e { "Failed to upload chunk $partIndex: ${response.status}" }
                // Decide if you want to break or retry
                return@onEach
            }
            partIndex++
        }.retry(3) { e ->
            (e is UnsupportedOperationException || e is IOException).also {
                log.i { "chunk upload exception e, retrying in 1 second" }
                if (it) delay(1.seconds)
            }
        }.catch { e ->
            when (e) {
                is UnsupportedOperationException, is IOException -> {
                    log.e { "Failed to upload file: ${e.message}" }
                }
                else -> throw e // rethrow unexpected exceptions
            }
        }.launchIn(this)
    }
}