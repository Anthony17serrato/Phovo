package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri

interface NetworkFile {
    val uri: Uri

    suspend fun exists(): Boolean

    suspend fun readBytes(): ByteArray?
}

expect fun getNetworkFile(uri: Uri): NetworkFile