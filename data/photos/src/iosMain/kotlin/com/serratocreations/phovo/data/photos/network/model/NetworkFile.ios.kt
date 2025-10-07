package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import com.serratocreations.phovo.core.model.network.MediaItemDto
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
    override val mediaItemDto: MediaItemDto,
    override val uri: String
) : NetworkFile, KoinComponent {
    private val ioDispatcher: CoroutineDispatcher by inject(IO_DISPATCHER)
    private val log = PhovoLogger.withTag("IosNetworkFile")

    override suspend fun exists(): Boolean {
        return resolveFileURL() != null
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> = flow {
        val fileURL = resolveFileURL() ?: run {
            log.e { "Cannot read file ${mediaItemDto.fileName}" }
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

    private suspend fun resolveFileURL(): NSURL? = withContext(ioDispatcher) {
        val assetId = uri.removePrefix("phasset://")
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetId), null)
        val asset = fetchResult.firstObject as? PHAsset ?: return@withContext null

        when (mediaItemDto.mediaType) {
            MediaType.Image -> {
                val options = PHContentEditingInputRequestOptions().apply {
                    networkAccessAllowed = true
                }

                return@withContext suspendCoroutine { continuation ->
                    asset.requestContentEditingInputWithOptions(options) { contentEditingInput, _ ->
                        continuation.resume(contentEditingInput?.fullSizeImageURL)
                    }
                }
            }
            MediaType.Video -> {
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
}

actual fun getNetworkFile(mediaItemDto: MediaItemDto, uri: String): NetworkFile = IosNetworkFile(mediaItemDto, uri)