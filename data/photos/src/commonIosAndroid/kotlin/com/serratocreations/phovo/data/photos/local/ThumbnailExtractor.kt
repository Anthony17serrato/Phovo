package com.serratocreations.phovo.data.photos.local

import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.ErrorResult
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger
import com.serratocreations.phovo.core.common.HIGH_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR

expect fun Image.compressToWebp(quality: Int): ByteArray

class ThumbnailExtractor(
    private val context: PlatformContext,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
    private val imageLoader: ImageLoader
) {

    suspend fun createLowResThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String
    ) {
        generateThumbnail(
            originalImageFile = originalImageFile,
            assetHash = assetHash,
            size = 32,
            quality = 60,
            targetDirName = LOW_RES_THUMBNAIL_DIR
        )
    }

    suspend fun createHighResThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String
    ) {
        generateThumbnail(
            originalImageFile = originalImageFile,
            assetHash = assetHash,
            size = 512,
            quality = 60,
            targetDirName = HIGH_RES_THUMBNAIL_DIR
        )
    }

    suspend fun generateThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String,
        size: Int,
        quality: Int,
        targetDirName: String
    ): Unit = withContext(ioDispatcher) {
        try {
            val thumbnailDir = FileKit.filesDir / targetDirName
            val thumbnailFile = PlatformFile(thumbnailDir, "$assetHash.webp")

            if (thumbnailFile.exists()) {
                return@withContext
            }

            val request = ImageRequest.Builder(context)
                .data(originalImageFile)
                .size(size)
                .build()

            when (val result = imageLoader.execute(request)) {
                is ErrorResult -> {
                    throw result.throwable
                }
                is SuccessResult -> {
                    val compressedBytes = result.image.compressToWebp(quality)
                    thumbnailDir.createDirectories(mustCreate = false)
                    thumbnailFile write compressedBytes
                }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "generateThumbnail Failed for $originalImageFile (size=$size): ${e.message}" }
        }
    }
}
