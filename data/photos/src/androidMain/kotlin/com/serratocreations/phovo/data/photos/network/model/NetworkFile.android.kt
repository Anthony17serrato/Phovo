package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri

actual fun getNetworkFile(uri: Uri, name: String): NetworkFile = AndroidNetworkFile(
    uri = uri,
    fileName = name
)