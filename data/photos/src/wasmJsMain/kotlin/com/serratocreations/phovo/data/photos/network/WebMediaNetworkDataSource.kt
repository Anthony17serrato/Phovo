package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.MediaItemDto
import com.serratocreations.phovo.data.photos.repository.model.SyncResult
import io.ktor.client.HttpClient

class WebMediaNetworkDataSource(
    client: HttpClient,
    logger: PhovoLogger
): MediaNetworkDataSource(client, logger) {
    override suspend fun chunkedUpload(
        mediaItemDto: MediaItemDto,
        mediaUri: String
    ): SyncResult {
        TODO("Not yet implemented")
    }
}