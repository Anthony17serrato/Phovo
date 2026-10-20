package com.serratocreations.phovo.data.photos.util

import com.serratocreations.phovo.core.model.MediaType
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHContentEditingInputRequestOptions
import platform.Photos.PHImageManager
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsVersionOriginal
import platform.Photos.cancelContentEditingInputRequest
import platform.Photos.requestContentEditingInputWithOptions
import kotlin.coroutines.resume

actual suspend fun MediaType.getPlatformFile(
    uri: String,
    ioDispatcher: CoroutineDispatcher
): PlatformFile? = withContext(ioDispatcher) {
    val assetId = uri.removePrefix("phasset://")
    val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetId), null)
    val asset = fetchResult.firstObject as? PHAsset ?: return@withContext null
    val url = when (this@getPlatformFile) {
        MediaType.Image -> fetchImageNSURL(asset)
        MediaType.Video -> fetchVideoNSURL(asset)
    } ?: return@withContext null
    PlatformFile(url)
}

/** The file Photos stores this asset in, or null when it is not on the device. */
internal suspend fun PHAsset.fetchFileUrl(): NSURL? = when (mediaType) {
    PHAssetMediaTypeImage -> fetchImageNSURL(this)
    PHAssetMediaTypeVideo -> fetchVideoNSURL(this)
    else -> null
}

private suspend fun fetchImageNSURL(asset: PHAsset): NSURL? = suspendCancellableCoroutine { continuation ->
    val options = PHContentEditingInputRequestOptions().apply {
        networkAccessAllowed = false
    }

    // Request the editing input, which contains the physical file URL
    val requestID = asset.requestContentEditingInputWithOptions(options) { input, _ ->
        // Resume the coroutine with the NSURL, or null if it failed
        continuation.resume(input?.fullSizeImageURL)
    }

    // Handle cancellation perfectly
    continuation.invokeOnCancellation {
        asset.cancelContentEditingInputRequest(requestID)
    }
}

private suspend fun fetchVideoNSURL(asset: PHAsset): NSURL? = suspendCancellableCoroutine { continuation ->
    val options = PHVideoRequestOptions().apply {
        networkAccessAllowed = false
        version = PHVideoRequestOptionsVersionOriginal
    }

    val requestID = PHImageManager.defaultManager().requestAVAssetForVideo(asset, options) { avAsset, _, _ ->
        val url = (avAsset as? AVURLAsset)?.URL
        continuation.resume(
            if (url != null && url.fileURL) {
                url
            } else null
        )
    }

    continuation.invokeOnCancellation {
        PHImageManager.defaultManager().cancelImageRequest(requestID)
    }
}