package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel

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
}