package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.core.model.network.NetworkResult
import com.serratocreations.phovo.data.photos.util.getPlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.source
import io.ktor.client.HttpClient
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.io.buffered

class IosAndroidMediaNetworkDataSource(
    client: HttpClient,
    logger: PhovoLogger,
    val ioDispatcher: CoroutineDispatcher
): MediaNetworkDataSource(client, logger) {
    private val log = logger.withTag("IosAndroidMediaNetworkDataSource")

    override suspend fun chunkedUpload(
        // TODO Pass platform file directly
        mediaItemDto: MediaItemDto,
        mediaUri: String,
        baseUrl: BaseUrl
    ): NetworkResult<Unit> {
        // TODO Clients need to be updated to use asset hash
        val file = mediaItemDto.mediaType.getPlatformFile(mediaUri, ioDispatcher) ?: return NetworkResult.NetworkError(
            message = "${log.tag} chunkedUpload could not get platform file for $mediaItemDto"
        )
        if (!file.exists()) {
            val errorMessage = "${log.tag} chunkedUpload file not found at $mediaUri"
            log.e { errorMessage }
            return NetworkResult.NetworkError(errorMessage)
        }

        // TODO chunking has been disabled, for photographs it is mostly not needed, in the future
        //  chunking should be used to allow videos to resume syncing if disruptions occur
        val byteReadChannel = ByteReadChannel(file.source().buffered())
        val response = syncChunk(
            chunk = byteReadChannel,
            fileName = mediaItemDto.fileName,
            partIndex = "1",
            baseUrl = baseUrl
        )

        return if (response.status.isSuccess()) {
            log.i { "Uploaded media ${mediaItemDto.assetHash}" }
            completeSuccessfulUpload(
                mediaItemDto = mediaItemDto,
                baseUrl = baseUrl
            )
        } else {
            val errorMessage = "Failed upload ${mediaItemDto.assetHash}: ${response.status}"
            log.e { errorMessage }
            NetworkResult.NetworkError(errorMessage)
        }
    }
}