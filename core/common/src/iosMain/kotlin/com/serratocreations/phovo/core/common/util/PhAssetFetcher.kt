package com.serratocreations.phovo.core.common.util

import coil3.Uri
import coil3.toUri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import platform.Foundation.NSData
import platform.Photos.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.resume

class PhAssetFetcher(
    private val data: Any,
    private val options: Options
) : Fetcher {
    companion object {
        private const val COMPRESSION = 0.8
        private val log = PhovoLogger.withTag("PhAssetFetcher")
    }

    override suspend fun fetch(): FetchResult? {
        log.i { "PhAssetFetcher fetch" }
        val asset = when (data) {
            is Uri -> data.toPhAsset()
            is PlatformFile -> {
                data.toPhAsset()
            }
            else -> null
        } ?: return null
        log.i { "PhAssetFetcher asset found ${asset.localIdentifier}" }
        val imageData = fetchImageData(asset) ?: return null
        log.i { ("PhAssetFetcher imageData found ${imageData.length}") }
        val source = ImageSource(
            source = Buffer().apply { write(imageData.toJpegBytes()) },
            fileSystem = options.fileSystem,
        )

        return SourceFetchResult(
            source = source,
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    private suspend fun fetchImageData(asset: PHAsset): NSData? = suspendCancellableCoroutine { continuation ->
        val options = PHImageRequestOptions().apply {
            this.networkAccessAllowed = true
            resizeMode = PHImageRequestOptionsResizeModeFast
        }

        val request = PHImageManager.defaultManager().requestImageDataForAsset(asset, options) { data, _, _, _ ->
            continuation.resume(data)
        }
        continuation.invokeOnCancellation {
            PHImageManager.defaultManager().cancelImageRequest(request)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toJpegBytes(): ByteArray = memScoped {
        val image = UIImage(data = this@toJpegBytes)
        return UIImageJPEGRepresentation(image, compressionQuality = COMPRESSION)!!.toByteArray()
    }
}

class PhAssetFetcherFactory : Fetcher.Factory<Any> {
    override fun create(data: Any, options: Options, imageLoader: coil3.ImageLoader): Fetcher? {
        val isSupported = when (data) {
            is Uri -> data.isPhAssetUri()
            is PlatformFile -> data.absolutePath().toUri().isPhAssetUri()
            else -> false
        }
        return if (isSupported) {
            PhAssetFetcher(data, options)
        } else null
    }
}
