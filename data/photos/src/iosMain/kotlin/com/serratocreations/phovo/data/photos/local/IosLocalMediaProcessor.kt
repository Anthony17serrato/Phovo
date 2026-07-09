package com.serratocreations.phovo.data.photos.local

import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.serratocreations.phovo.core.common.HIGH_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.util.phAssetUriFromLocalId
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import com.serratocreations.phovo.data.photos.util.segregate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.write
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.valueForKey
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHAssetResource
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHContentEditingInputRequestOptions
import platform.Photos.PHFetchOptions
import platform.Photos.PHPhotoLibrary
import platform.Photos.cancelContentEditingInputRequest
import platform.Photos.requestContentEditingInputWithOptions
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.EncodedImageFormat
import coil3.toBitmap
import kotlinx.coroutines.CancellationException

class IosLocalMediaProcessor(
    private val fileHashCalculator: FileHashCalculator,
    private val logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher,
    private val imageLoader: coil3.ImageLoader
) : LocalMediaProcessor {
    private val log = PhovoLogger.withTag("IosLocalMediaProcessor")

    override fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        processMediaChannel: SendChannel<MediaItem>
    ) = launch {
        val authorizationStatus = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        when(authorizationStatus) {
            PHAuthorizationStatusAuthorized -> {
                log.i { "PHPhotoLibrary authorized" }
                processAuthorizedItems(processedItems, processMediaChannel)
            }
            PHAuthorizationStatusLimited -> {
                log.i { "PHPhotoLibrary limited" }
                processAuthorizedItems(processedItems, processMediaChannel)
            }
            PHAuthorizationStatusNotDetermined -> {
                log.w { "PHPhotoLibrary permission not determined" }
                requestPermissions()
                processAuthorizedItems(processedItems, processMediaChannel)
            }
            PHAuthorizationStatusRestricted -> {
                log.w { "PHPhotoLibrary restricted permission" }
            }
            PHAuthorizationStatusDenied -> {
                log.w { "PHPhotoLibrary permission denied" }
            }
        }
    }

    private fun CoroutineScope.processAuthorizedItems(
        processedItems: List<MediaItem>,
        processMediaChannel: SendChannel<MediaItem>
    ) {
        val (processedVideos, processedImages) = processedItems.segregate()
        fetchImages(processedImages)
            .onEach { processMediaChannel.send(it) }
            .launchIn(this)

        fetchVideos(processedVideos)
            .onEach { processMediaChannel.send(it) }
            .launchIn(this)
    }

    private suspend fun requestPermissions(): Boolean = suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { status ->
            continuation.resume(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited)
        }
        continuation.invokeOnCancellation {
            // request does not have a cancel API, do nothing
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class, ExperimentalUuidApi::class)
    private fun fetchImages(processedImages: List<MediaImageItem>): Flow<MediaImageItem> = flow {
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val imageItems = mutableListOf<PHAsset>()
        val processedImageHashes = processedImages.map { it.uniqueAssetIdentifier }
        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            imageItems.add(obj as PHAsset)
        }
        log.i { "IosPhovoItemDao images $imageItems" }
        imageItems.forEach { asset ->
            val assetPlatformFile = PlatformFile(phAssetUriFromLocalId(asset.localIdentifier).toString())
            val assetUri = AssetLocation.LocalAssetLocation(assetPlatformFile)
            val fullSizeAssetNsurl = fetchAssetNSURL(asset = asset) ?: run {
                log.e { "Could not get full size asset for $assetUri" }
                return@forEach
            }
            val fullSizeAssetFile = PlatformFile(fullSizeAssetNsurl)
            val assetHash = fileHashCalculator.computeSha256(fullSizeAssetFile)
            if (assetHash in processedImageHashes) return@forEach
            createLowResThumbnail(assetPlatformFile, assetHash = assetHash)
            createHighResThumbnail(assetPlatformFile, assetHash = assetHash)

            val resource = PHAssetResource.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource ?: return@forEach
            val name = resource.originalFilename
            val instant = asset.creationDate?.toKotlinInstant()
            // TODO: Instead of excluding images where date could not be determined parse the date from exif data
            val localDateTime = instant?.toLocalDateTime(TimeZone.currentSystemDefault())
                ?: return@forEach

            val size = resource.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L
            emit(MediaImageItem(
                assetLocation = assetUri,
                fileName = name,
                dateInFeed = localDateTime,
                size = bytes,
                uniqueAssetIdentifier = assetHash,
                isSynced = false
            ))
        }
    }.flowOn(ioDispatcher)

    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class, ExperimentalUuidApi::class)
    private fun fetchVideos(processedVideos: List<MediaVideoItem>): Flow<MediaVideoItem> = flow {
        val fetchOptions = PHFetchOptions()
        val videoItems = mutableListOf<PHAsset>()
        val processedVideoHashes = processedVideos.map { it.uniqueAssetIdentifier }
        val videoAssets =
            PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeVideo, fetchOptions)
        videoAssets.enumerateObjectsUsingBlock { obj, _, _ ->
            videoItems.add(obj as PHAsset)
        }
        log.i { "IosPhovoItemDao fetchVideos $videoItems" }
        videoItems.forEach { asset ->
            val assetPlatformFile = PlatformFile(phAssetUriFromLocalId(asset.localIdentifier).toString())
            val assetUri = AssetLocation.LocalAssetLocation(assetPlatformFile)
            val fullSizeAssetNsurl = fetchAssetNSURL(asset = asset) ?: run {
                log.e { "Could not get full size asset for $assetUri" }
                return@forEach
            }
            val fullSizeAssetFile = PlatformFile(fullSizeAssetNsurl)
            val assetHash = fileHashCalculator.computeSha256(fullSizeAssetFile)
            if (assetHash in processedVideoHashes) return@forEach

            createLowResThumbnail(assetPlatformFile, assetHash = assetHash)
            createHighResThumbnail(assetPlatformFile, assetHash = assetHash)

            val resource = PHAssetResource.assetResourcesForAsset(asset)
                .firstOrNull() as? PHAssetResource ?: return@forEach
            val name = resource.originalFilename
            val instant = asset.creationDate?.toKotlinInstant()
            val localDateTime = instant?.toLocalDateTime(TimeZone.currentSystemDefault())
                ?: return@forEach
            val size = resource.valueForKey("fileSize") as? NSNumber
            val bytes = size?.longValue ?: 0L
            emit(
                MediaVideoItem(
                    assetLocation = assetUri,
                    fileName = name,
                    dateInFeed = localDateTime,
                    size = bytes,
                    duration = asset.duration.toLong().seconds,
                    uniqueAssetIdentifier = assetHash,
                    isSynced = false
                )
            )
        }
    }.flowOn(ioDispatcher)

    private suspend fun fetchAssetNSURL(asset: PHAsset): NSURL? = suspendCancellableCoroutine { continuation ->
        val options = PHContentEditingInputRequestOptions().apply {
            networkAccessAllowed = false
        }

        // Request the editing input, which contains the physical file URL
        val requestID = asset.requestContentEditingInputWithOptions(options) { input, _ ->
            // Resume the coroutine with the NSURL, or null if it failed
            continuation.resume(input?.fullSizeImageURL)
        }

        // Handle cancellation perfectly
        continuation.invokeOnCancellation {
            asset.cancelContentEditingInputRequest(requestID)
        }
    }

    override suspend fun createLowResThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String
    ) {
        generateThumbnail(
            originalImageFile = originalImageFile,
            assetHash = assetHash,
            size = 32.0,
            quality = 0.6,
            targetDirName = LOW_RES_THUMBNAIL_DIR
        )
    }

    override suspend fun createHighResThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String
    ) {
        generateThumbnail(
            originalImageFile = originalImageFile,
            assetHash = assetHash,
            size = 512.0,
            quality = 0.6,
            targetDirName = HIGH_RES_THUMBNAIL_DIR
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun generateThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String,
        size: Double,
        quality: Double,
        targetDirName: String
    ): Unit = withContext(ioDispatcher) {
        try {
            val thumbnailDir = FileKit.filesDir / targetDirName
            val thumbnailFile = PlatformFile(thumbnailDir, "$assetHash.jpg")

            if (thumbnailFile.exists()) {
                return@withContext
            }

            val request = coil3.request.ImageRequest.Builder(coil3.PlatformContext.INSTANCE)
                .data(originalImageFile)
                .size(size.toInt())
                .build()

            when (val result = imageLoader.execute(request)) {
                is ErrorResult -> {
                    throw result.throwable
                }
                is SuccessResult -> {
                    val bitmap = result.image.toBitmap()
                    val skiaImage = SkiaImage.makeFromBitmap(bitmap)
                    val data = skiaImage.encodeToData(EncodedImageFormat.JPEG, (quality * 100).toInt())
                    val compressedBytes = data?.bytes

                    if (compressedBytes != null) {
                        thumbnailDir.createDirectories(mustCreate = false)
                        thumbnailFile write compressedBytes
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.e { "generateThumbnail Failed for $originalImageFile (size=$size): $e" }
        }
    }
}