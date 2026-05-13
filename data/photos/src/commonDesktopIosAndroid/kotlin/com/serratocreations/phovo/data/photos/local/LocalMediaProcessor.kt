package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exceptions.FileKitException
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext

/**
 * Processes Locally available media. Processed media becomes available for user to view and
 * sync across all of their devices.
 */
interface LocalMediaProcessor {
    /**
     * Processes locally stored media items by extracting metadata for items which we do not have
     * metadata for.
     * @param processedItems The items which have already been processed previously by the processing
     * Job.
     * @param localDirectory The directory where media is stored on the desktop client.
     * (null for all other clients)
     * @return A receive channel of processed media items
     */
    fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        localDirectory: String?,
        processMediaChannel: SendChannel<MediaItem>
    ): Job

    suspend fun createThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String,
        ioDispatcher: CoroutineDispatcher,
        maxSize: Int = 64
    ): Unit = withContext(ioDispatcher) {
        try {
            // Read the original image
            val imageBytes = originalImageFile.readBytes()

            // Create a thumbnail by compressing and resizing
            val thumbnailBytes = FileKit.compressImage(
                bytes = imageBytes,
                quality = 85,
                maxWidth = maxSize,
                maxHeight = maxSize,
                imageFormat = ImageFormat.JPEG
            )

            // Save the thumbnail
            val thumbnailDir = FileKit.filesDir / LOW_RES_THUMBNAIL_DIR
            thumbnailDir.createDirectories(mustCreate = false)
            val thumbnailFile =
                PlatformFile(thumbnailDir,
                    "$assetHash.jpg"
                )
            thumbnailFile write thumbnailBytes
        } catch (e: FileKitException) {
            PhovoLogger.withTag("LocalMediaProcessor").e(throwable = e) {
                "createThumbnail Failed to extract thumbnail for $originalImageFile"
            }
        }
    }
}