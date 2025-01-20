package com.serratocreations.phovo.data.photos.network.model

import android.content.Context
import coil3.Uri
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import android.net.Uri as AndroidUri

class AndroidNetworkFile(
    override val uri: Uri,
    val fileName: String
) : NetworkFile, KoinComponent {
    private val context: Context by inject()
    private val ioDispatcher: CoroutineDispatcher by inject(qualifier = named(IO_DISPATCHER))

    private val androidUri = AndroidUri.parse(uri.toString())

    override suspend fun fileName(): String = fileName

    override suspend fun exists(): Boolean = withContext(ioDispatcher) {
        // Check if the URI can be opened
        return@withContext try {
            context.contentResolver.openInputStream(androidUri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun readBytes(): ByteArray? = withContext(ioDispatcher) {
        // Read bytes directly from the content URI
        return@withContext context.contentResolver.openInputStream(androidUri)?.use { it.readBytes() }
    }
}