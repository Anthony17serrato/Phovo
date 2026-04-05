package com.serratocreations.phovo.feature.photos.util

import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.serratocreations.phovo.core.common.util.toByteArray
import com.serratocreations.phovo.core.common.util.toPhAsset
import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.resume

class PhAssetFetcher(data: Any, options: Options) : PlatformFetcher(data, options) {
    companion object {
        private const val COMPRESSION = 0.8
        private val log = PhovoLogger.withTag("PhAssetFetcher")
    }

    override suspend fun fetch(): FetchResult? {
        log.i { "PhAssetFetcher fetch" }
        val uri = data as Uri
        val asset = uri.toPhAsset() ?: return null
        log.i { "PhAssetFetcher asset found ${asset.localIdentifier}" }
        val imageData = fetchImageData(asset) ?: return null
        log.i { ("PhAssetFetcher imageData found ${imageData.length}") }
        val source = ImageSource(
            source = Buffer().apply { write(imageData.toJpegBytes()) },
            fileSystem = options.fileSystem,
        )

        return SourceFetchResult(
            source = source,
            mimeType = null, // Or dynamically determine
            dataSource = DataSource.DISK
        )
    }

    private suspend fun fetchImageData(asset: PHAsset): NSData? = suspendCancellableCoroutine { continuation ->
        val options = PHImageRequestOptions().apply {
            this.networkAccessAllowed = true
            resizeMode = PHImageRequestOptionsResizeModeFast
        }

        PHImageManager.defaultManager().requestImageDataForAsset(asset, options) { data, _, _, _ ->
            continuation.resume(data)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toJpegBytes(): ByteArray = memScoped {
        val image = UIImage(data = this@toJpegBytes)

        return UIImageJPEGRepresentation(image, compressionQuality = COMPRESSION)!!.toByteArray()
    }
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = this.size.toULong()
            )
        }
    }
}