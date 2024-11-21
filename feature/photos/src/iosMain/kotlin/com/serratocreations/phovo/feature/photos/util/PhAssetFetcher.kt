package com.serratocreations.phovo.feature.photos.util

import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.coroutines.resume

class PhAssetFetcher(data: Any, options: Options) : PlatformFetcher(data, options) {
    companion object {
        private const val COMPRESSION = 0.8
        private val HEIF_HEADER_FTYP = "ftyp".encodeUtf8()
        private val HEIF_BRAND_MIF1 = "mif1".encodeUtf8()
        private val HEIF_BRAND_HEIC = "heic".encodeUtf8()
        private val HEIF_BRAND_HEIX = "heix".encodeUtf8()
        private val HEIF_BRAND_AVIF = "avif".encodeUtf8()
        private val HEIF_BRAND_AVIS = "avis".encodeUtf8()
    }

    override suspend fun fetch(): FetchResult? {
        println("PhAssetFetcher fetch")
        val uri = data as Uri
        val localIdentifier = uri.toString().removePrefix("phasset://")
        val asset = fetchAssetByIdentifier(localIdentifier) ?: throw IllegalArgumentException("PHAsset not found.")
        println("PhAssetFetcher asset found ${asset.localIdentifier}")
        val imageData = fetchImageData(asset) ?: return null
        println("PhAssetFetcher imageData found ${imageData.length}")
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

    private fun fetchAssetByIdentifier(localIdentifier: String): PHAsset? {
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localIdentifier), null)
        return fetchResult.firstObject() as? PHAsset
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

    /**
     * Return 'true' if the [source] contains a HEIF image. The [source] is not consumed.
     */
    private fun isHeif(source: BufferedSource): Boolean {
        // Check if 'ftyp' exists at offset 4
        return source.rangeEquals(4, HEIF_HEADER_FTYP) &&
                (source.rangeEquals(8, HEIF_BRAND_MIF1) || // Single image HEIF
                        source.rangeEquals(8, HEIF_BRAND_HEIC) || // HEIF with HEVC codec
                        source.rangeEquals(8, HEIF_BRAND_HEIX) || // HEIF extension
                        source.rangeEquals(8, HEIF_BRAND_AVIF) || // HEIF with AV1 codec
                        source.rangeEquals(8, HEIF_BRAND_AVIS))   // AV1 Image Sequence
    }

    // Move to common util if useful
    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.length.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return byteArray
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