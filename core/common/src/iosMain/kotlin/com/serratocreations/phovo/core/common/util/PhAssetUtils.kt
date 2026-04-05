package com.serratocreations.phovo.core.common.util

import coil3.Uri
import coil3.toUri
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import platform.Photos.PHAsset

const val PH_ASSET_URI_SCHEME = "phasset"

fun Uri.isPhAssetUri(): Boolean =
    this.scheme == PH_ASSET_URI_SCHEME

fun phAssetUriFromLocalId(localId: String): Uri =
    "$PH_ASSET_URI_SCHEME://$localId".toUri()

fun localIdFromPhAssetUri(uri: Uri): String =
    if (uri.isPhAssetUri()) {
        uri.toString().removePrefix("$PH_ASSET_URI_SCHEME://")
    } else uri.toString()

/**
 * Returns a [PHAsset] if the fetch API was able to retrieve one for the provided [PlatformFile]
 */
fun PlatformFile.toPhAsset(): PHAsset? {
    val videoUri = this.absolutePath().toUri()
    return videoUri.toPhAsset()
}

/**
 * Returns a [PHAsset] if the fetch API was able to retrieve one for the provided [PlatformFile]
 */
fun Uri.toPhAsset(): PHAsset? {
    val localId = localIdFromPhAssetUri(this)
    val assets = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localId), null)
    return assets.firstObject as? PHAsset
}