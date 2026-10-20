package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.channels.SendChannel

/**
 * Processes Locally available media. Processed media becomes available for user to view and
 * sync across all of their devices.
 */
interface LocalMediaProcessor {
    /**
     * Processes locally stored media items by extracting metadata for items which we do not have
     * metadata for.
     * @return A receive channel of processed media items
     */
    suspend fun scanLocalItems(
        processMediaSendChannel: SendChannel<PlatformFile>
    )

    suspend fun processItem(mediaItem: PlatformFile, assetHash: String): MediaProcessingResult
}