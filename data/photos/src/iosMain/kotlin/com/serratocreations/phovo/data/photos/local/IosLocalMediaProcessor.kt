package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.core.common.util.phAssetUriFromLocalId
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHAssetResource
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHFetchOptions
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

class IosLocalMediaProcessor(
    logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher
) : LocalMediaProcessor {
    private val log = logger.withTag("IosPhovoItemDao")

    override fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        localDirectory: String?,
        processMediaChannel: SendChannel<MediaItem>
    ): Job {
        TODO("Not yet implemented")
    }

    fun processLocalItems(localDirectory: String?): Flow<List<MediaItem>> {
        return flow {
            requestPhotoLibraryPermission()
            val status = PHPhotoLibrary.Companion.authorizationStatus()
            log.i { "Photo Library Authorization Status: $status" }
            val localImagesAndVideos: List<MediaItem> = fetchImages() + fetchVideos()
            emit(localImagesAndVideos)
        }
    }

    private suspend fun requestPhotoLibraryPermission() = suspendCoroutine { continuation ->
        PHPhotoLibrary.Companion.requestAuthorization { status ->
            when (status) {
                PHAuthorizationStatusAuthorized -> {
                    log.i { "Photo Library access granted" }
                    // Proceed with photo fetching logic
                }

                PHAuthorizationStatusDenied -> {
                    log.w { "Photo Library access denied" }
                    // Guide the user to settings if needed
                }

                PHAuthorizationStatusRestricted -> {
                    log.w { "Photo Library access restricted" }
                    // Handle the case for restricted access (e.g., parental controls)
                }

                PHAuthorizationStatusNotDetermined -> {
                    log.w { "Photo Library access not determined" }
                    // The user hasn't been asked yet, possibly retry request
                }
            }
            continuation.resume(Unit)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun fetchImages(): List<MediaImageItem> = withContext(ioDispatcher) {
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.Companion.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val imageItems = mutableListOf<MediaImageItem>()

        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            val instant = asset.creationDate?.toKotlinInstant()
            // TODO: Instead of excluding images where date could not be determined parse the date from exif data
            val localDateTime = instant?.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
                ?: return@enumerateObjectsUsingBlock
            val resource = PHAssetResource.Companion.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource
            val name = resource?.originalFilename ?: ""
            val size = resource?.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L
            val phovoImageItem = MediaImageItem(
                uri = phAssetUriFromLocalId(asset.localIdentifier),
                name = name,
                dateInFeed = localDateTime,
                size = bytes.toInt()
            )
            imageItems.add(phovoImageItem)
        }
        log.i { "IosPhovoItemDao images $imageItems" }
        return@withContext imageItems.toList()
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun fetchVideos(): List<MediaVideoItem> = withContext(ioDispatcher) {
        val fetchOptions = PHFetchOptions()
        val videoItems = mutableListOf<MediaVideoItem>()
        val videoAssets =
            PHAsset.Companion.fetchAssetsWithMediaType(PHAssetMediaTypeVideo, fetchOptions)
        videoAssets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            val instant = asset.creationDate?.toKotlinInstant()
            val localDateTime = instant?.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
                ?: return@enumerateObjectsUsingBlock

            val resource = PHAssetResource.Companion.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource
            val name = resource?.originalFilename ?: ""
            val size = resource?.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L

            val videoItem = MediaVideoItem(
                uri = phAssetUriFromLocalId(asset.localIdentifier),
                name = name,
                dateInFeed = localDateTime,
                size = bytes.toInt(),
                duration = asset.duration.toLong().seconds
            )
            videoItems.add(videoItem)
        }
        log.i { "IosPhovoItemDao fetchVideos $videoItems" }
        return@withContext videoItems.toList()
    }
}