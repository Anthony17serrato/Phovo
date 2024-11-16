package com.serratocreations.phovo.feature.photos.util

import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import platform.Foundation.NSData
import platform.Photos.*
import platform.posix.memcpy
import kotlin.coroutines.resume

class PhAssetFetcher(data: Any, options: Options) : PlatformFetcher(data, options) {

    override suspend fun fetch(): FetchResult? {
        println("PhAssetFetcher fetch")
        val uri = data as Uri
        val localIdentifier = uri.toString().removePrefix("phasset://")
        val asset = fetchAssetByIdentifier(localIdentifier) ?: throw IllegalArgumentException("PHAsset not found.")
        println("PhAssetFetcher asset found ${asset.localIdentifier}")
        val imageData = fetchImageData(asset) ?: return null
        println("PhAssetFetcher imageData found ${imageData.length}")
        val source = ImageSource(
            source = Buffer().apply { write(imageData.toByteArray()) },
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

    @OptIn(ExperimentalForeignApi::class)
    fun NSData.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.length.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return byteArray
    }
}