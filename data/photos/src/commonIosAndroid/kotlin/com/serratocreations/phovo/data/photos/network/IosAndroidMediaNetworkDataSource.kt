package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.network.model.getNetworkFile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.seconds

class IosAndroidMediaNetworkDataSource(
    private val client: HttpClient,
    logger: PhovoLogger
): MediaNetworkDataSource(
    client,
    logger
) {
    private val log = logger.withTag("IosAndroidMediaNetworkDataSource")

    suspend fun syncMedia(mediaItemDto: MediaItemDto): SyncResult = coroutineScope {
        log.i { "syncMedia $mediaItemDto" }
        val file = getNetworkFile(mediaItemDto)
        if (!file.exists()) {
            log.e { "File not found at ${mediaItemDto.localUri}" }
            return@coroutineScope SyncError
        }

        // Step 1: Init
        try {
            client.post("http://10.0.0.183:8080/upload/init") {
                contentType(ContentType.Application.Json)
                setBody(mediaItemDto)
            }
        } catch (e: IOException) {
            log.e { "error initializing upload $e" }
            return@coroutineScope SyncError
        }

        // Step 2: Chunks
        var partIndex = 0
        return@coroutineScope async {
            var result: SyncResult = SyncError
            file.readInChunks().onEach { fileChunk ->
                log.i { "Uploading chunk $partIndex size=${fileChunk.size}" }

                val response = client.post("http://10.0.0.183:8080/upload/chunk") {
                    header("X-File-Name", mediaItemDto.fileName)
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
                     result = completeSuccessfulUpload(mediaItemDto = mediaItemDto)
                }
            }.collect()
            return@async result
        }.await()
    }

    private suspend fun completeSuccessfulUpload(mediaItemDto: MediaItemDto): SyncResult {
        try {
            // Step 3: Complete
            val updatedItem: MediaItemDto = client.post("http://10.0.0.183:8080/upload/complete") {
                contentType(ContentType.Text.Plain)
                setBody(mediaItemDto.localUuid)
            }.body()

            log.i { "Upload complete for ${updatedItem.fileName} (remoteUri=${updatedItem.remoteUri})" }
            return SyncSuccessful(updatedItem)
        } catch (e: IOException) {
            log.e { "Upload completion failed: ${e.message}" }
            return SyncError
        }
    }
}

sealed interface SyncResult

data class SyncSuccessful(val updatedMediaItemDto: MediaItemDto): SyncResult

data object SyncError: SyncResult