package com.serratocreations.phovo.data.photos.local

import com.serratocreations.phovo.core.common.util.phAssetUriFromLocalId
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.repository.util.segregate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    ) = launch {
        requestPhotoLibraryPermission()
        val status = PHPhotoLibrary.Companion.authorizationStatus()
        log.i { "Photo Library Authorization Status: $status" }
        val (processedVideos, processedImages) = processedItems.segregate()
        fetchImages(processedImages).onEach { processedImage ->
            processMediaChannel.send(processedImage)
        }.launchIn(this)
        fetchVideos(processedVideos).onEach { processedVideo ->
            processMediaChannel.send(processedVideo)
        }.launchIn(this)
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
    private fun fetchImages(processedImages: List<MediaImageItem>): Flow<MediaImageItem> = flow {
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.Companion.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val imageItems = mutableListOf<PHAsset>()
        val processedImageIds = processedImages.map { it.fileName }
        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            imageItems.add(obj as PHAsset)
        }
        log.i { "IosPhovoItemDao images $imageItems" }
        imageItems.forEach { asset ->
            val resource = PHAssetResource.Companion.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource ?: return@forEach
            val name = resource.originalFilename
            if (name in processedImageIds) return@forEach
            val instant = asset.creationDate?.toKotlinInstant()
            // TODO: Instead of excluding images where date could not be determined parse the date from exif data
            val localDateTime = instant?.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
                ?: return@forEach

            val size = resource.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L
            emit(MediaImageItem(
                uri = phAssetUriFromLocalId(asset.localIdentifier),
                fileName = name,
                dateInFeed = localDateTime,
                size = bytes.toInt()
            ))
        }
    }.flowOn(ioDispatcher)

    @OptIn(ExperimentalForeignApi::class)
    private fun fetchVideos(processedVideos: List<MediaVideoItem>): Flow<MediaVideoItem> = flow {
        val fetchOptions = PHFetchOptions()
        val videoItems = mutableListOf<PHAsset>()
        val processedVideoIds = processedVideos.map { it.fileName }
        val videoAssets =
            PHAsset.Companion.fetchAssetsWithMediaType(PHAssetMediaTypeVideo, fetchOptions)
        videoAssets.enumerateObjectsUsingBlock { obj, _, _ ->
            videoItems.add(obj as PHAsset)
        }
        log.i { "IosPhovoItemDao fetchVideos $videoItems" }
        videoItems.forEach { asset ->
            val resource = PHAssetResource.Companion.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource ?: return@forEach
            val name = resource.originalFilename
            if (name in processedVideoIds) return@forEach
            val instant = asset.creationDate?.toKotlinInstant()
            val localDateTime = instant?.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
                ?: return@forEach
            val size = resource.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L
            emit(
                MediaVideoItem(
                    uri = phAssetUriFromLocalId(asset.localIdentifier),
                    fileName = name,
                    dateInFeed = localDateTime,
                    size = bytes.toInt(),
                    duration = asset.duration.toLong().seconds
                )
            )
        }
    }.flowOn(ioDispatcher)
}