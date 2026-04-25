package com.serratocreations.phovo.data.photos.repository.model

import coil3.toUri
import com.serratocreations.phovo.data.photos.network.Endpoint
import io.github.vinceglb.filekit.PlatformFile

/**
 * Wrapper for the location that a media item can be accessed from. If the asset is available locally
 * the local version will be used. If the asset is available remotely the remote version will be used.
 * If the asset is available both locally and remotely the local version will be preferred.
 */
sealed interface AssetLocation {
    data class LocalAssetLocation(
        val localAssetLocation: PlatformFile
    ): AssetLocation

    data object RemoteAssetLocation: AssetLocation {
        fun getAssetUri(
            assetHash: String,
            endpoint: Endpoint,
            baseUrl: String
        ) = "$baseUrl${endpoint.value}${assetHash}".toUri()
    }
}