package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri

actual fun getNetworkFile(uri: Uri): NetworkFile = AndroidNetworkFile(uri = uri)