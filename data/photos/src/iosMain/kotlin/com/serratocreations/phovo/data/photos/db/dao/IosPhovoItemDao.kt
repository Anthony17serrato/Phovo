package com.serratocreations.phovo.data.photos.db.dao

import kotlinx.coroutines.flow.Flow
import com.serratocreations.phovo.core.common.util.phAssetUriFromLocalId
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoVideoItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.Photos.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

class IosPhovoItemDao(
    logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher
) : PhovoItemDao {
    private val log = logger.withTag("IosPhovoItemDao")

    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        return flow {
            requestPhotoLibraryPermission()
            val status = PHPhotoLibrary.authorizationStatus()
            log.i { "Photo Library Authorization Status: $status" }
            val localImagesAndVideos: List<PhovoItem> = fetchImages() + fetchVideos()
            emit(localImagesAndVideos)
        }
    }

    private suspend fun requestPhotoLibraryPermission() = suspendCoroutine { continuation ->
        PHPhotoLibrary.requestAuthorization { status ->
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
    private suspend fun fetchImages(): List<PhovoImageItem> = withContext(ioDispatcher) {
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val imageItems = mutableListOf<PhovoImageItem>()

        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            val instant = asset.creationDate?.toKotlinInstant()
            // TODO: Instead of excluding images where date could not be determined parse the date from exif data
            val localDateTime = instant?.toLocalDateTime(TimeZone.currentSystemDefault()) ?: return@enumerateObjectsUsingBlock
            val resource = PHAssetResource.assetResourcesForAsset(asset).firstOrNull() as? PHAssetResource
            val name = resource?.originalFilename ?: ""
            val size = resource?.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L
            val phovoImageItem = PhovoImageItem(
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
    private suspend fun fetchVideos(): List<PhovoVideoItem> = withContext(ioDispatcher) {
        val fetchOptions = PHFetchOptions()
        val videoItems = mutableListOf<PhovoVideoItem>()
        val videoAssets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeVideo, fetchOptions)
        videoAssets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            val instant = asset.creationDate?.toKotlinInstant()
            val localDateTime = instant?.toLocalDateTime(TimeZone.currentSystemDefault()) ?: return@enumerateObjectsUsingBlock

            val resource = PHAssetResource.assetResourcesForAsset(asset).firstOrNull() as? PHAssetResource
            val name = resource?.originalFilename ?: ""
            val size = resource?.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L

            val videoItem = PhovoVideoItem(
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