package com.serratocreations.phovo.data.photos.local

import coil3.ImageLoader
import com.serratocreations.phovo.core.common.util.phAssetUriFromLocalId
import com.serratocreations.phovo.core.common.util.toPhAsset
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaDimensions
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingFailure
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.util.fetchFileUrl
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import platform.Photos.PHFetchOptions
import kotlin.time.Duration.Companion.seconds

class IosLocalMediaProcessor(
    private val logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher,
    private val imageLoader: ImageLoader
) : LocalMediaProcessor {
    private val log = PhovoLogger.withTag("IosLocalMediaProcessor")
    private val thumbnailExtractor = ThumbnailExtractor(
        context = coil3.PlatformContext.INSTANCE,
        imageLoader = imageLoader,
        ioDispatcher = ioDispatcher,
        logger = logger
    )

    override suspend fun scanLocalItems(
        processMediaSendChannel: SendChannel<PlatformFile>
    ) = coroutineScope {
        fetchImages()
            .onEach { processMediaSendChannel.send(it) }
            .launchIn(this)

        fetchVideos()
            .onEach { processMediaSendChannel.send(it) }
            .launchIn(this)
        Unit
    }


    @OptIn(ExperimentalForeignApi::class)
    private fun fetchImages(): Flow<PlatformFile> = flow {
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val imageItems = mutableListOf<PHAsset>()
        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            imageItems.add(obj as PHAsset)
        }
        log.i { "IosPhovoItemDao images $imageItems" }
        imageItems.forEach { asset ->
            emitIfOnDevice(asset)
        }
    }.flowOn(ioDispatcher)

    override suspend fun processItem(
        mediaItem: PlatformFile,
        assetHash: String
    ): MediaProcessingResult = withContext(ioDispatcher) {
        val asset = mediaItem.toPhAsset()
            ?: return@withContext MediaProcessingResult.Failure(MediaProcessingFailure.MissingFromLibrary)
        when (asset.mediaType) {
            PHAssetMediaTypeImage -> processImage(mediaItem, asset, assetHash)
            PHAssetMediaTypeVideo -> processVideo(mediaItem, asset, assetHash)
            else -> MediaProcessingResult.Failure(MediaProcessingFailure.UnsupportedType)
        }
    }

    private suspend fun processImage(file: PlatformFile, asset: PHAsset, assetHash: String): MediaProcessingResult {
        val dimensions = asset.displayDimensions()
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.UnreadableDimensions)
        val resource = PHAssetResource.assetResourcesForAsset(asset).firstOrNull() as? PHAssetResource
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.MissingFromLibrary)
        // TODO: Instead of excluding images where date could not be determined parse the date from exif data
        val dateInFeed = asset.creationDate?.toKotlinInstant()
            ?.toLocalDateTime(TimeZone.currentSystemDefault())
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.MissingCreationDate)

        thumbnailExtractor.createLowResThumbnail(file, assetHash = assetHash)
        thumbnailExtractor.createHighResThumbnail(file, assetHash = assetHash)

        val size = (resource.valueForKey("fileSize") as? NSNumber)?.longValue ?: 0L
        return MediaProcessingResult.Success(
            MediaImageItem(
                assetLocation = AssetLocation.LocalAssetLocation(file),
                fileName = resource.originalFilename,
                dateInFeed = dateInFeed,
                size = size,
                uniqueAssetIdentifier = assetHash,
                isSynced = false,
                dimensions = dimensions
            )
        )
    }

    //@OptIn(ExperimentalTime::class)
    private suspend fun processVideo(
        file: PlatformFile,
        asset: PHAsset,
        assetHash: String
    ): MediaProcessingResult {
        val dimensions = asset.displayDimensions()
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.UnreadableDimensions)
        val resource = PHAssetResource.assetResourcesForAsset(asset).firstOrNull() as? PHAssetResource
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.MissingFromLibrary)
        val dateInFeed = asset.creationDate?.toKotlinInstant()
            ?.toLocalDateTime(TimeZone.currentSystemDefault())
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.MissingCreationDate)

        thumbnailExtractor.createLowResThumbnail(file, assetHash = assetHash)
        thumbnailExtractor.createHighResThumbnail(file, assetHash = assetHash)

        val size = (resource.valueForKey("fileSize") as? NSNumber)?.longValue ?: 0L
        return MediaProcessingResult.Success(
            MediaVideoItem(
                assetLocation = AssetLocation.LocalAssetLocation(file),
                fileName = resource.originalFilename,
                dateInFeed = dateInFeed,
                size = size,
                duration = asset.duration.toLong().seconds,
                uniqueAssetIdentifier = assetHash,
                isSynced = false,
                dimensions = dimensions
            )
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun fetchVideos(): Flow<PlatformFile> = flow {
        val fetchOptions = PHFetchOptions()
        val videoItems = mutableListOf<PHAsset>()
        val videoAssets =
            PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeVideo, fetchOptions)
        videoAssets.enumerateObjectsUsingBlock { obj, _, _ ->
            videoItems.add(obj as PHAsset)
        }
        log.i { "IosPhovoItemDao fetchVideos $videoItems" }
        videoItems.forEach { asset ->
            emitIfOnDevice(asset)
        }
    }.flowOn(ioDispatcher)

    /**
     * Emits the asset's phasset:// file. Assets whose file is not on the device, such as ones only
     * stored in iCloud, are skipped because there is nothing to hash.
     */
    private suspend fun FlowCollector<PlatformFile>.emitIfOnDevice(asset: PHAsset) {
        val assetPlatformFile = PlatformFile(phAssetUriFromLocalId(asset.localIdentifier).toString())
        if (asset.fetchFileUrl() == null) {
            log.e { "Could not get full size asset for $assetPlatformFile" }
            return
        }
        emit(assetPlatformFile)
    }
}

/**
 * Photos reports the size the asset is displayed at, orientation and edits included, which matches
 * the current version the viewer shows. Returns null when it reports no size, which fails
 * processing for the asset.
 */
private fun PHAsset.displayDimensions(): MediaDimensions? {
    val width = pixelWidth.toInt()
    val height = pixelHeight.toInt()
    return if (width > 0 && height > 0) MediaDimensions(width, height) else null
}
