package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.domain.mapper.toMediaItemWithThumbnails
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class ClientGetPhotosFeedWithThumbnailsUseCase(
    private val mediaRepository: MediaRepository,
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val ioDispatcher: CoroutineDispatcher,
    logger: PhovoLogger
): GetPhotosFeedWithThumbnailsUseCase {

    override operator fun invoke(): Flow<List<MediaItemWithThumbnails>> {

        return combine(
            mediaRepository.phovoMediaFlow(),
            serverConfigRepository.observeServerConfig().distinctUntilChanged()
        ) { mediaList, serverConfig ->
            return@combine mediaList.mapNotNull { mediaItem ->
                // Prefer file thumb if exists, fallback to network thumb, no thumb if no base url
                val lowResThumb = (FileKit.filesDir / LOW_RES_THUMBNAIL_DIR / "${mediaItem.uniqueAssetIdentifier}.webp").let {
                    if (it.exists()) {
                        AssetLocation.LocalAssetLocation(it)
                    } else if (mediaItem.isSynced) {
                        AssetLocation.RemoteAssetLocation
                    } else {
                        null
                    }
                }
                // If asset is stored locally, check if we have a background-generated high-res thumbnail.
                // Fallback to the original local asset file path if it hasn't been generated yet.
                val highResThumb = if (mediaItem.assetLocation is AssetLocation.LocalAssetLocation) {
                    val cachedHighResFile = FileKit.filesDir / com.serratocreations.phovo.core.common.HIGH_RES_THUMBNAIL_DIR / "${mediaItem.uniqueAssetIdentifier}.webp"
                    if (cachedHighResFile.exists()) {
                        AssetLocation.LocalAssetLocation(cachedHighResFile)
                    } else {
                        mediaItem.assetLocation
                    }
                } else {
                    AssetLocation.RemoteAssetLocation
                }

                mediaItem.toMediaItemWithThumbnails(
                    lowResThumbnailLocation = lowResThumb,
                    highResThumbnailLocation = highResThumb,
                    assetHash = mediaItem.uniqueAssetIdentifier,
                    baseUrl = serverConfig?.serverBaseUrlString
                )
            }
        }.flowOn(ioDispatcher)
    }
}