package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri

interface NetworkFile {
    val uri: Uri
    val fileName: String?

    fun exists(): Boolean

    fun readBytes(): ByteArray
}

expect fun getNetworkFile(uri: Uri, name: String): NetworkFile