package com.serratocreations.phovo.core.domain

import coil3.Uri
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn


class GetPhotosFeedWithThumbnailsUseCase(
    private val mediaRepository: MediaRepository,
    private val serverConfigRepository: ServerConfigRepository,
    private val ioDispatcher: CoroutineDispatcher,
    logger: PhovoLogger
) {
    private val log = logger.withTag("GetPhotosFeedWithThumbnailsUseCase")

    operator fun invoke(): Flow<List<MediaItemWithThumbnails>> {

        return combine(
            mediaRepository.phovoMediaFlow(),
            serverConfigRepository.observeServerConfig().distinctUntilChanged()
        ) { mediaList, serverConfig ->
            val backupDirectory = serverConfig?.backupDirectory
            mediaList.map { mediaItem ->
                val thumbnailFiles = backupDirectory?.let { backupDirectoryNotNull ->
                    getThumbnailFiles(
                        backupDirectoryNotNull,
                        dataUuid = mediaItem.localUuid
                    )
                } ?: run {
                    log.w { "There is no backup directory" }
                    null
                }
                mediaItem.toMediaItemWithThumbnails(thumbnailFiles)
            }
        }.flowOn(ioDispatcher)
    }
}

internal expect fun getThumbnailFiles(
    rootOutputDirectory: PlatformFile,
    dataUuid: String
): ThumbnailResources

fun MediaItem.toMediaItemWithThumbnails(thumbnailResources: ThumbnailResources?): MediaItemWithThumbnails {
    return when(this) {
        is MediaImageItem -> {
            MediaItemWithThumbnails.MediaImageItem(
                uri = uri,
                lowResThumbnail = thumbnailResources?.lowResThumbnailDirectory,
                thumbnailUri = thumbnailResources?.highResThumbnail ?: uri,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                localUuid = localUuid,
                remoteUuid = remoteUuid
            )
        }
        is MediaVideoItem -> {
            MediaItemWithThumbnails.MediaVideoItem(
                uri = uri,
                lowResThumbnail = thumbnailResources?.lowResThumbnailDirectory,
                thumbnailUri = thumbnailResources?.highResThumbnail ?: uri,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                localUuid = localUuid,
                remoteUuid = remoteUuid,
                duration = duration
            )
        }
    }
}

data class ThumbnailResources(
    val lowResThumbnailDirectory: PlatformFile?,
    val highResThumbnail: Uri?
)