package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import kotlinx.coroutines.CoroutineScope

/**
 * Contract for APIs that are supported by a subset of project platforms.
 * These platforms must process items that are stored locally.(Android, Desktop, & Ios platforms)
 */
abstract class LocalSupportMediaRepository(
    remotePhotosDataSource: MediaNetworkDataSource,
    appScope: CoroutineScope
): MediaRepository(
    remotePhotosDataSource = remotePhotosDataSource,
    appScope = appScope
) {
    /**
     * API initializes job to process local media and synchronize to server.
     * Processing includes tasks such as extracting media metadata and generating md5 hashes and
     * deduplication logic
     */
    abstract fun initMediaProcessing(localDirectory: String?)
}