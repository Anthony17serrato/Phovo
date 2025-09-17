package com.serratocreations.phovo.data.photos.network.model

import coil3.Uri
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.*
import platform.Photos.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosNetworkFile(
    override val uri: Uri
) : NetworkFile, KoinComponent {
    private val ioDispatcher: CoroutineDispatcher by inject(IO_DISPATCHER)
    private val log = PhovoLogger.withTag("IosNetworkFile")

    override suspend fun exists(): Boolean {
        return resolveFileURL() != null
    }

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> {
        TODO("Not yet implemented")
//        val fileURL = resolveFileURL() ?: run {
//            log.e { "Cannot read file at $uri" }
//            return null
//        }
//        val data = NSData.dataWithContentsOfURL(fileURL) ?: run {
//            log.e { "Cannot read file at $uri" }
//            return null
//        }
//
//        return data.toByteArray()
    }

    // TODO Verify IOS sets filename with extension to MediaItem
    suspend fun fileName(): String = getFileName()

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
            log.e { "Invalid URI or file does not exist: $uri" }
            null
        }
        return fileURL?.lastPathComponent ?: "Unknown"
    }
}

actual fun getNetworkFile(uri: Uri): NetworkFile = IosNetworkFile(uri)