package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.mapper.toMediaItemWithThumbnails
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.serverconfig.DesktopServerConfigRepository
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
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
            // Because the server is the remote data source assets should always be local, if a remote
            // asset exists it will be filtered(this is an error state)
            return@combine mediaList.mapNotNull { mediaItem ->
                val rootOutputDirectory =
                    serverConfig?.backupDirectory ?: return@mapNotNull mediaItem.toMediaItemWithThumbnails(
                        lowResThumbnailLocation = null,
                        highResThumbnailLocation = mediaItem.assetLocation,
                        assetHash = mediaItem.uniqueAssetIdentifier,
                        baseUrl = null
                    )
                val lowResThumbDir = rootOutputDirectory / GetPhotosFeedWithThumbnailsUseCase.LOW_RES_THUMBNAIL_DIR
                val lowResThumb = (lowResThumbDir / "${mediaItem.uniqueAssetIdentifier}.webp").let {
                    if (it.exists()) {
                        AssetLocation.LocalAssetLocation(localAssetLocation = it)
                    } else { null }
                }
                val highResThumbDir = rootOutputDirectory / GetPhotosFeedWithThumbnailsUseCase.HIGH_RES_THUMBNAIL_DIR
                val highResThumb = (highResThumbDir / "${mediaItem.uniqueAssetIdentifier}.webp").let {
                    if (it.exists()) {
                        AssetLocation.LocalAssetLocation(localAssetLocation = it)
                    } else { mediaItem.assetLocation }
                }
                mediaItem.toMediaItemWithThumbnails(
                    lowResThumbnailLocation = lowResThumb,
                    highResThumbnailLocation = highResThumb,
                    assetHash = mediaItem.uniqueAssetIdentifier,
                    baseUrl = null
                )
            }
        }
    }
}