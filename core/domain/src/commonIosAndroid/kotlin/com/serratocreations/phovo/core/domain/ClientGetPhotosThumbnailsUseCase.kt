package com.serratocreations.phovo.core.domain

import coil3.toUri
import com.serratocreations.phovo.core.domain.mapper.toMediaItemWithThumbnails
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
import com.serratocreations.phovo.data.server.data.repository.IosAndroidWasmServerConfigRepository
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
    private val serverConfigRepository: IosAndroidWasmServerConfigRepository,
    private val ioDispatcher: CoroutineDispatcher,
    logger: PhovoLogger
): GetPhotosFeedWithThumbnailsUseCase {

    override operator fun invoke(): Flow<List<MediaItemWithThumbnails>> {

        return combine(
            mediaRepository.phovoMediaFlow(),
            serverConfigRepository.observeServerConfig().distinctUntilChanged()
        ) { mediaList, serverConfig ->
            return@combine mediaList.map { mediaItem ->
                // Prefer file thumb if exists, fallback to network thumb, no thumb if no base url
                val lowResThumb = (FileKit.filesDir / GetPhotosFeedWithThumbnailsUseCase.LOW_RES_THUMBNAIL_DIR / "${mediaItem.localUuid}.webp").let {
                    if (it.exists()) { LocalOrRemoteAsset.LocalAsset(it) } else {
                        serverConfig?.serverBaseUrlString?.let { baseUrlNotNull ->
                            val remoteUri = (baseUrlNotNull + GetPhotosFeedWithThumbnailsUseCase.LOW_RES_THUMBNAIL_API + mediaItem.localUuid).toUri()
                            LocalOrRemoteAsset.RemoteAsset(remoteUri)
                        }
                    }
                }
                // If asset is stored locally pass asset directly, if not get high-res thumb from server
                val highResThumb = if (mediaItem.assetLocation is LocalOrRemoteAsset.LocalAsset) {
                    mediaItem.assetLocation
                } else {
                    serverConfig?.serverBaseUrlString?.let { baseUrlNotNull ->
                        val remoteUri = (baseUrlNotNull + GetPhotosFeedWithThumbnailsUseCase.HIGH_RES_THUMBNAIL_API + mediaItem.localUuid).toUri()
                        LocalOrRemoteAsset.RemoteAsset(remoteUri)
                    } ?: mediaItem.assetLocation // fallback to asset location, this should never happen
                }

                mediaItem.toMediaItemWithThumbnails(
                    lowResThumbnailLocation = lowResThumb,
                    highResThumbnailLocation = highResThumb
                )
            }
        }.flowOn(ioDispatcher)
    }
}