package com.serratocreations.phovo.data.photos.local

import coil3.ImageLoader
import com.serratocreations.phovo.core.common.util.interleave
import com.serratocreations.phovo.core.common.util.phAssetUriFromLocalId
import com.serratocreations.phovo.core.common.util.toPhAsset
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaDimensions
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingFailure
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.local.model.ScanEvent
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.util.fetchFileUrl
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaType
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHAssetResource
import platform.Photos.PHFetchOptions
import platform.Photos.PHFetchResult
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

    override fun scanLocalItems(): Flow<ScanEvent> = flow {
        val imageAssets = fetchAssets(PHAssetMediaTypeImage)
        val videoAssets = fetchAssets(PHAssetMediaTypeVideo)
        val combinedCount = (imageAssets.count + videoAssets.count).toInt()
        emit(ScanEvent.Found(count = combinedCount))
        // Mapped before interleaving so each producer looks up its next file while it waits its turn
        val scanEvents = interleave(
            imageAssets.assets().map { it.toScanEvent() },
            videoAssets.assets().map { it.toScanEvent() }
        )
        emitAll(scanEvents)
    }.flowOn(ioDispatcher)

    private fun fetchAssets(mediaType: PHAssetMediaType): PHFetchResult =
        PHAsset.fetchAssetsWithMediaType(mediaType, PHFetchOptions())

    /** Reads the assets lazily, one index at a time, instead of copying the whole result first. */
    private fun PHFetchResult.assets(): Flow<PHAsset> = flow {
        val result = this@assets
        for (index in 0uL until result.count) {
            emit(result.objectAtIndex(index) as PHAsset)
        }
    }

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

    /**
     * Maps the asset to its phasset:// file. Assets whose file is not on the device, such as ones
     * only stored in iCloud, are skipped because there is nothing to hash, and reported as
     * [ScanEvent.Skipped].
     */
    private suspend fun PHAsset.toScanEvent(): ScanEvent {
        val assetPlatformFile = PlatformFile(phAssetUriFromLocalId(localIdentifier).toString())
        if (fetchFileUrl() == null) {
            log.e { "Could not get full size asset for $assetPlatformFile" }
            return ScanEvent.Skipped
        }
        return ScanEvent.Item(assetPlatformFile)
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
