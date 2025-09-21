package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.AVFoundation.AVURLAsset
import platform.Foundation.*
import platform.Photos.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosNetworkFile(
    override val mediaItem: MediaItem
) : NetworkFile, KoinComponent {
    private val ioDispatcher: CoroutineDispatcher by inject(IO_DISPATCHER)
    private val log = PhovoLogger.withTag("IosNetworkFile")

    override suspend fun exists(): Boolean {
        return resolveFileURL() != null
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> = flow {
        val fileURL = resolveFileURL() ?: run {
            log.e { "Cannot read file ${mediaItem.fileName}" }
            return@flow
        }

        val inputStream = NSInputStream.inputStreamWithURL(fileURL)
            ?: run {
                log.e { "Failed to open input stream for $fileURL" }
                return@flow
            }

        inputStream.open()
        try {
            val buffer = UByteArray(chunkSize)
            buffer.usePinned { pinned ->
                while (true) {
                    val read = inputStream.read(pinned.addressOf(0), buffer.size.toULong())
                    if (read <= 0) break
                    val byteArray = ByteArray(read.toInt()) { i ->
                        buffer[i].toByte()
                    }
                    emit(byteArray)
                }
            }
        } finally {
            inputStream.close()
        }
    }.flowOn(ioDispatcher)

    // TODO Verify IOS sets filename with extension to MediaItem
    suspend fun fileName(): String = getFileName()

    private suspend fun resolveFileURL(): NSURL? = withContext(ioDispatcher) {
        val assetId = mediaItem.uri.toString().removePrefix("phasset://")
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetId), null)
        val asset = fetchResult.firstObject as? PHAsset ?: return@withContext null

        when (mediaItem) {
            is MediaImageItem -> {
                val options = PHContentEditingInputRequestOptions().apply {
                    networkAccessAllowed = true
                }

                return@withContext suspendCoroutine { continuation ->
                    asset.requestContentEditingInputWithOptions(options) { contentEditingInput, _ ->
                        continuation.resume(contentEditingInput?.fullSizeImageURL)
                    }
                }
            }
            is MediaVideoItem -> {
                val options = PHVideoRequestOptions().apply {
                    networkAccessAllowed = true
                    version = PHVideoRequestOptionsVersionOriginal
                }
                suspendCoroutine { cont ->
                    PHImageManager.defaultManager().requestAVAssetForVideo(asset, options) { avAsset, _, _ ->
                        val url = (avAsset as? AVURLAsset)?.URL
                        cont.resume(
                            if (url != null && url.fileURL) {
                                url
                            } else null
                        )
                    }
                }
            }
        }
    }

    private suspend fun getFileName(): String {
        val fileURL = resolveFileURL() ?: run {
            log.e { "Invalid URI or file does not exist: ${mediaItem.uri}" }
            null
        }
        return fileURL?.lastPathComponent ?: "Unknown"
    }
}

actual fun getNetworkFile(mediaItem: MediaItem): NetworkFile = IosNetworkFile(mediaItem)