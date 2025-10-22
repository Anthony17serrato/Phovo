package com.serratocreations.phovo.data.photos.util

import com.serratocreations.phovo.core.model.MediaType
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHContentEditingInputRequestOptions
import platform.Photos.PHImageManager
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsVersionOriginal
import platform.Photos.requestContentEditingInputWithOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual suspend fun MediaType.getPlatformFile(
    uri: String,
    ioDispatcher: CoroutineDispatcher
): PlatformFile? = withContext(ioDispatcher) {
    val assetId = uri.removePrefix("phasset://")
    val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetId), null)
    val asset = fetchResult.firstObject as? PHAsset ?: return@withContext null
    return@withContext when(this@getPlatformFile) {
        MediaType.Image -> {
            val options = PHContentEditingInputRequestOptions().apply {
                networkAccessAllowed = false // or true if you want iCloud fetches
            }

            val imageUrl = suspendCoroutine<NSURL?> { cont ->
                asset.requestContentEditingInputWithOptions(options) { input, _ ->
                    cont.resume(input?.fullSizeImageURL)
                }
            } ?: return@withContext null
            PlatformFile(imageUrl)
        }
        MediaType.Video -> {
            val options = PHVideoRequestOptions().apply {
                networkAccessAllowed = false
                version = PHVideoRequestOptionsVersionOriginal
            }
            val videoUrl = suspendCoroutine<NSURL?> { cont ->
                PHImageManager.defaultManager().requestAVAssetForVideo(asset, options) { avAsset, _, _ ->
                    val url = (avAsset as? AVURLAsset)?.URL
                    cont.resume(
                        if (url != null && url.fileURL) {
                            url
                        } else null
                    )
                }
            } ?: return@withContext null
            PlatformFile(videoUrl)
        }
    }
}