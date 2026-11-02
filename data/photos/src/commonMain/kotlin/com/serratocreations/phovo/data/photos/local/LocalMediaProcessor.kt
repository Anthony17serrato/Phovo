package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.local.model.ScanEvent
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

/**
 * Processes Locally available media. Processed media becomes available for user to view and
 * sync across all of their devices.
 */
interface LocalMediaProcessor {
    /**
     * Scans locally stored media. Each query reports how many assets it matched, then the assets
     * to process, and any it skipped. Cold: collecting it runs the scan.
     */
    fun scanLocalItems(): Flow<ScanEvent>

    suspend fun processItem(mediaItem: PlatformFile, assetHash: String): MediaProcessingResult
}
