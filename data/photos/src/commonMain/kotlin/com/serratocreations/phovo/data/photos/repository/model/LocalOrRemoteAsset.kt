package com.serratocreations.phovo.data.photos.repository.model

import coil3.Uri
import io.github.vinceglb.filekit.PlatformFile

/**
 * Wrapper for the location that a media item can be accessed from. If the asset is available locally
 * the local version will be used. If the asset is available remotely the remote version will be used.
 * If the asset is available both locally and remotely the local version will be preferred.
 */
sealed interface LocalOrRemoteAsset {
    data class LocalAsset(
        val localAssetLocation: PlatformFile,
        // TODO remove an move to core model
        val isAlsoAvailableRemotely: Boolean
    ): LocalOrRemoteAsset

    data class RemoteAsset(
        val remoteAssetUri: Uri
    ): LocalOrRemoteAsset
}