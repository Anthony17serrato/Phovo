package com.serratocreations.phovo.core.common.util

import coil3.Uri
import coil3.toUri

const val PH_ASSET_URI_SCHEME = "phasset"

fun Uri.isPhAssetUri(): Boolean =
    this.scheme == PH_ASSET_URI_SCHEME

fun phAssetUriFromLocalId(localId: String): Uri =
    "$PH_ASSET_URI_SCHEME://$localId".toUri()

fun localIdFromPhAssetUri(uri: Uri): String =
    if (uri.isPhAssetUri()) {
        uri.toString().removePrefix("phasset://")
    } else uri.toString()