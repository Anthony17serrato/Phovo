package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toMediaItemWithThumbnails
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ServerGetPhotosFeedWithThumbnailsUseCase(
    private val mediaRepository: MediaRepository,
    private val serverConfigRepository: DesktopServerConfigRepository,
    logger: PhovoLogger
): GetPhotosFeedWithThumbnailsUseCase {
    override fun invoke(): Flow<List<MediaItemWithThumbnails>> {
        return combine(
            mediaRepository.phovoMediaFlow(),
            serverConfigRepository.observeServerConfig().distinctUntilChanged()
        ) { mediaList, serverConfig ->
            return@combine mediaList.map { mediaItem ->
                val rootOutputDirectory =
                    serverConfig?.backupDirectory ?: return@map mediaItem.toMediaItemWithThumbnails(
                        lowResThumbnailLocation = null,
                        highResThumbnailLocation = mediaItem.assetLocation
                    )
                val lowResThumbDir = rootOutputDirectory / GetPhotosFeedWithThumbnailsUseCase.LOW_RES_THUMBNAIL_DIR
                val lowResThumb = (lowResThumbDir / "${mediaItem.localUuid}.webp").let {
                    if (it.exists()) LocalOrRemoteAsset.LocalAsset(it) else null
                }
                val highResThumbDir = rootOutputDirectory / GetPhotosFeedWithThumbnailsUseCase.HIGH_RES_THUMBNAIL_DIR
                val highResThumb = (highResThumbDir / "${mediaItem.localUuid}.webp").let {
                    if (it.exists()) LocalOrRemoteAsset.LocalAsset(it) else mediaItem.assetLocation
                }
                mediaItem.toMediaItemWithThumbnails(
                    lowResThumbnailLocation = lowResThumb,
                    highResThumbnailLocation = highResThumb
                )
            }
        }
    }
}