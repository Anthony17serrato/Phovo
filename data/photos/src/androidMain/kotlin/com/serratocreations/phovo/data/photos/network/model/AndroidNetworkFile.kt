package com.serratocreations.phovo.data.photos.network.model

import android.content.Context
import coil3.Uri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.net.Uri as AndroidUri

class AndroidNetworkFile(
    override val uri: Uri,
    override val fileName: String
) : NetworkFile, KoinComponent {
    private val context: Context by inject()
    private val androidUri = AndroidUri.parse(uri.toString())

    override fun exists(): Boolean {
        // Check if the URI can be opened
        return try {
            context.contentResolver.openInputStream(androidUri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun readBytes(): ByteArray {
        // Read bytes directly from the content URI
        return context.contentResolver.openInputStream(androidUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Unable to open URI: $uri")
    }
}