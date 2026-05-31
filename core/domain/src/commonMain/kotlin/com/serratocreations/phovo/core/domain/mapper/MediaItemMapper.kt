package com.serratocreations.phovo.core.domain.mapper

import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import com.serratocreations.phovo.core.model.network.ApiEndpoints
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem

/**
 * Mapper returns null if the asset is only available remotely and a base URL has not been
 * provided.
 */
fun MediaItem.toMediaItemWithThumbnails(
    lowResThumbnailLocation: AssetLocation?,
    highResThumbnailLocation: AssetLocation,
    assetHash: String,
    baseUrl: BaseUrl?
): MediaItemWithThumbnails? {
    return when(this) {
        is MediaImageItem -> {
            MediaItemWithThumbnails.MediaImageItem(
                assetLocation = assetLocation.toDomainAssetLocation(
                    assetHash = assetHash,
                    endpoint = ApiEndpoints.SOURCE_FILE_API,
                    baseUrl = baseUrl
                ) ?: return null,
                lowResThumbnailLocation = lowResThumbnailLocation?.toDomainAssetLocation(
                    assetHash = assetHash,
                    endpoint = ApiEndpoints.LOW_RES_THUMBNAIL_API,
                    baseUrl = baseUrl
                ),
                highResThumbnailLocation = highResThumbnailLocation.toDomainAssetLocation(
                    assetHash = assetHash,
                    endpoint = ApiEndpoints.HIGH_RES_THUMBNAIL_API,
                    baseUrl = baseUrl
                ) ?: return null,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                localUuid = uniqueAssetIdentifier
            )
        }
        is MediaVideoItem -> {
            MediaItemWithThumbnails.MediaVideoItem(
                assetLocation = assetLocation.toDomainAssetLocation(
                    assetHash = assetHash,
                    endpoint = ApiEndpoints.GET_ALL_MEDIA_API,
                    baseUrl = baseUrl
                ) ?: return null,
                lowResThumbnailLocation = lowResThumbnailLocation?.toDomainAssetLocation(
                    assetHash = assetHash,
                    endpoint = ApiEndpoints.LOW_RES_THUMBNAIL_API,
                    baseUrl = baseUrl
                ),
                highResThumbnailLocation = highResThumbnailLocation.toDomainAssetLocation(
                    assetHash = assetHash,
                    endpoint = ApiEndpoints.HIGH_RES_THUMBNAIL_API,
                    baseUrl = baseUrl
                ) ?: return null,
                fileName = fileName,
                dateInFeed = dateInFeed,
                size = size,
                localUuid = uniqueAssetIdentifier,
                duration = duration
            )
        }
    }
}