package com.serratocreations.phovo.core.domain.model

import coil3.Uri
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import io.github.vinceglb.filekit.PlatformFile

/**
 * Wrapper for the location that a media item can be accessed from. If the asset is available locally
 * the local version will be used. If the asset is available remotely the remote version will be used.
 * If the asset is available both locally and remotely the local version will be preferred.
 *
 * Differs from [AssetLocation] in that it provides the remote asset URI without needing
 * to pass assetHash or baseUrl
 */
sealed interface DomainAssetLocation {
    data class LocalAssetLocation(
        val localAssetLocation: PlatformFile
    ): DomainAssetLocation

    data class RemoteAssetLocation(
        val remoteAssetUri: Uri
    ): DomainAssetLocation
}