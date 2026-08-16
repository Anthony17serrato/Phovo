package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.domain.mapper.toMediaItemWithThumbnails
import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.serverconfig.ServerEndpointResolver
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample

class ClientGetPhotosFeedWithThumbnailsUseCase(
    private val mediaRepository: MediaRepository,
    private val endpointResolver: ServerEndpointResolver,
    private val ioDispatcher: CoroutineDispatcher,
    logger: PhovoLogger
): GetPhotosFeedWithThumbnailsUseCase {

    @OptIn(FlowPreview::class)
    override operator fun invoke(): Flow<List<MediaItemWithThumbnails>> {

        return combine(
            // Sampled upstream of the mapping below, see PHOTOS_FEED_SAMPLE_PERIOD
            mediaRepository.phovoMediaFlow().sample(PHOTOS_FEED_SAMPLE_PERIOD),
            // Thumbnail URLs must point at wherever the server is *now*, so this tracks the
            // resolved address rather than the stored config, which no longer carries one.
            endpointResolver.state
        ) { mediaList, connectionState ->
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
                    baseUrl = (connectionState as? ServerConnectionState.Connected)?.baseUrl
                )
            }
        }.flowOn(ioDispatcher)
    }
}