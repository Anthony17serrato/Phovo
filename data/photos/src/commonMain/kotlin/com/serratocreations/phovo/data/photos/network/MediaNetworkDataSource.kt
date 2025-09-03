package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.network.model.getNetworkFile
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.IOException
import org.koin.core.annotation.Singleton

@Singleton
class MediaNetworkDataSource(
    private val client: HttpClient,
    logger: PhovoLogger
) {
    private val log = logger.withTag("PhotosNetworkDataSource")

    // TODO: Implement network API for getting all items
    fun allItemsFlow(): Flow<List<MediaItem>> = flowOf()

    suspend fun syncMedia(mediaItem: MediaItem) {
//        val response: HttpResponse = client.post("http://10.0.0.71:8080/upload") {
//            contentType(ContentType.Application.Json)
//            setBody(imageItem)
//        }
        log.i { "syncImage $mediaItem" }
        val file = getNetworkFile(mediaItem.uri)

        if (!file.exists()) {
            log.e { "File not found at ${mediaItem.uri}" }
            return
        }

        try {
            // TODO: Getting OOM when trying to sync large video files
            if (mediaItem is MediaVideoItem) return
            val bytes = file.readBytes() ?: throw UnsupportedOperationException("Could not read bytes for $file")
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "http://10.0.0.253:8080/upload",//http://10.0.0.204:8080/upload"
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${mediaItem.fileName}")
                    })
                }
            )
            log.i { "Response: ${response.status}" }
        } catch (e: Exception) {
            when (e) {
                is UnsupportedOperationException, is IOException -> {
                    log.e { "Failed to upload file: ${e.message}" }
                }
                else -> {
                    // rethrow any other exceptions
                    throw e
                }
            }
        }
    }
}