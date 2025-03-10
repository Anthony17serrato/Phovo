package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri
import com.serratocreations.phovo.core.common.di.IoDispatcher
import com.serratocreations.phovo.core.common.util.toByteArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import platform.Foundation.*
import platform.Photos.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosNetworkFile(
    override val uri: Uri
) : NetworkFile, KoinComponent {
    private val ioDispatcher: CoroutineDispatcher by inject(named<IoDispatcher>())

    override suspend fun exists(): Boolean {
        return resolveFileURL() != null
    }

    override suspend fun fileName(): String = getFileName()

    override suspend fun readBytes(): ByteArray? {
        val fileURL = resolveFileURL() ?: run {
            println("Cannot read file at $uri")
            return null
        }
        val data = NSData.dataWithContentsOfURL(fileURL) ?: run {
            println("Cannot read file at $uri")
            return null
        }

        return data.toByteArray()
    }

    private suspend fun resolveFileURL(): NSURL? = withContext(ioDispatcher) {
        val assetId = uri.toString().removePrefix("phasset://")
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetId), null)
        val asset = fetchResult.firstObject as? PHAsset ?: return@withContext null

        val options = PHContentEditingInputRequestOptions().apply {
            networkAccessAllowed = true
        }

        return@withContext suspendCoroutine { continuation ->
            asset.requestContentEditingInputWithOptions(options) { contentEditingInput, _ ->
                continuation.resume(contentEditingInput?.fullSizeImageURL)
            }
        }
    }

    private suspend fun getFileName(): String {
        val fileURL = resolveFileURL() ?: run {
            println("Invalid URI or file does not exist: $uri")
            null
        }
        return fileURL?.lastPathComponent ?: "Unknown"
    }
}

actual fun getNetworkFile(uri: Uri, name: String): NetworkFile = IosNetworkFile(uri)