package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaMetadata
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.network.model.getNetworkFile
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
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
        log.i { "syncMedia $mediaItem" }
        val file = getNetworkFile(mediaItem.uri)
        if (!file.exists()) {
            log.e { "File not found at ${mediaItem.uri}" }
            return@coroutineScope
        }

        // Step 1: Init
        val metadata = MediaMetadata(
            fileName = mediaItem.fileName,
            size = mediaItem.size.toLong(),
            mediaType = when (mediaItem) {
                is MediaImageItem -> MediaType.Image
                is MediaVideoItem -> MediaType.Video
            }
        )
        try {
            client.post("http://10.0.0.183:8080/upload/init") {
                contentType(ContentType.Application.Json)
                setBody(metadata)
            }
        } catch (e: IOException) {
            log.e { "error initializing upload $e" }
            return@coroutineScope
        }

        // Step 2: Chunks
        var partIndex = 0
        file.readInChunks().onEach { fileChunk ->
            log.i { "Uploading chunk $partIndex size=${fileChunk.size}" }

            val response = client.post("http://10.0.0.183:8080/upload/chunk") {
                header("X-File-Name", mediaItem.fileName)
                header("X-Chunk-Index", partIndex.toString())
                setBody(fileChunk)
            }

            if (response.status.isSuccess()) {
                log.i { "Uploaded chunk $partIndex" }
            } else {
                log.e { "Failed chunk $partIndex: ${response.status}" }
                throw IOException("Failed to upload chunk $partIndex: ${response.status}")
            }
            partIndex++
        }.retry(3) { e ->
            (e is IOException).also {
                log.w { "Retrying after 1 second error: ${e.message}" }
                if (it) delay(1.seconds)
            }
        }.catch { e ->
            log.e { "Upload failed: ${e.message}" }
            throw e
        }.onCompletion { cause ->
            if (cause == null) {
                completeSuccessfulUpload(fileName = mediaItem.fileName)
            }
        }.launchIn(this)
    }

    private suspend fun completeSuccessfulUpload(fileName: String) {
        try {
            // Step 3: Complete
            client.post("http://10.0.0.183:8080/upload/complete") {
                setBody(fileName)
            }
            log.i { "Upload complete for $fileName" }
        } catch (e: IOException) {
            log.e { "Upload completion failed: ${e.message}" }
        }
    }
}