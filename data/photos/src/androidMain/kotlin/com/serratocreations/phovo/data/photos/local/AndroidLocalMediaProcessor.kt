package com.serratocreations.phovo.data.photos.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.exifinterface.media.ExifInterface
import coil3.ImageLoader
import com.serratocreations.phovo.core.common.extension.androidUri
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingFailure
import com.serratocreations.phovo.data.photos.local.model.MediaProcessingResult
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.mimeType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AndroidLocalMediaProcessor(
    private val ioDispatcher: CoroutineDispatcher,
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val logger: PhovoLogger
) : LocalMediaProcessor {
    private val log = logger.withTag("AndroidLocalMediaProcessor")
    private val resolver = context.contentResolver
    private val thumbnailExtractor = ThumbnailExtractor(
        context = context,
        imageLoader = imageLoader,
        ioDispatcher = ioDispatcher,
        logger = logger
    )
    private val dimensionsReader = AndroidMediaDimensionsReader(
        context = context,
        ioDispatcher = ioDispatcher
    )

    override suspend fun scanLocalItems(
        processMediaSendChannel: SendChannel<PlatformFile>
    ) = coroutineScope {
        // TODO add observability of updates(Probably by registering Broadcast receiver)
        queryImages()
            .onEach { processedImage ->
                processMediaSendChannel.send(processedImage)
            }.launchIn(this)

        queryVideos()
            .onEach { processedVideo ->
                processMediaSendChannel.send(processedVideo)
            }.launchIn(this)
        Unit
    }

    override suspend fun processItem(
        mediaItem: PlatformFile,
        assetHash: String
    ): MediaProcessingResult = withContext(ioDispatcher) {
        val uri = mediaItem.androidFile.androidUri
        when (mediaItem.mimeType()?.primaryType?.lowercase()) {
            "image" -> processImage(mediaItem, uri, assetHash)
            "video" -> processVideo(mediaItem, uri, assetHash)
            else -> MediaProcessingResult.Failure(MediaProcessingFailure.UnsupportedType)
        }
    }

    private class ImageRow(
        val fileName: String,
        val size: Long,
        val dateTaken: Duration?,
        val dateAdded: Duration
    )

    private suspend fun processImage(file: PlatformFile, uri: Uri, assetHash: String): MediaProcessingResult {
        val dimensions = dimensionsReader.readImageDimensions(uri)
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.UnreadableDimensions)
        val row = querySingleRow(
            uri,
            arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED
            )
        ) { cursor ->
            ImageRow(
                fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)),
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)),
                dateTaken = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))?.milliseconds,
                dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)).seconds
            )
        } ?: return MediaProcessingResult.Failure(MediaProcessingFailure.MissingFromLibrary)

        thumbnailExtractor.createLowResThumbnail(file, assetHash)
        thumbnailExtractor.createHighResThumbnail(file, assetHash)
        val dateInFeed = row.dateTaken?.utcMsToLocalDateTime()
            ?: resolver.parseDateTakenFromExif(uri)
            ?: row.dateAdded.utcMsToLocalDateTime()

        return MediaProcessingResult.Success(
            MediaImageItem(
                assetLocation = AssetLocation.LocalAssetLocation(file),
                isSynced = false,
                fileName = row.fileName,
                dateInFeed = dateInFeed,
                size = row.size,
                uniqueAssetIdentifier = assetHash,
                dimensions = dimensions
            )
        )
    }

    private class VideoRow(
        val fileName: String,
        val size: Long,
        val dateTaken: Duration?,
        val dateAdded: Duration,
        val duration: Duration
    )

    private suspend fun processVideo(file: PlatformFile, uri: Uri, assetHash: String): MediaProcessingResult {
        val dimensions = dimensionsReader.readVideoDimensions(uri)
            ?: return MediaProcessingResult.Failure(MediaProcessingFailure.UnreadableDimensions)
        val row = querySingleRow(
            uri,
            arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION
            )
        ) { cursor ->
            VideoRow(
                fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)),
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
                dateTaken = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))?.milliseconds,
                dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)).seconds,
                duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)).milliseconds
            )
        } ?: return MediaProcessingResult.Failure(MediaProcessingFailure.MissingFromLibrary)

        thumbnailExtractor.createLowResThumbnail(file, assetHash)
        thumbnailExtractor.createHighResThumbnail(file, assetHash)
        val dateInFeed = row.dateTaken?.utcMsToLocalDateTime()
            ?: row.dateAdded.utcMsToLocalDateTime()

        return MediaProcessingResult.Success(
            MediaVideoItem(
                assetLocation = AssetLocation.LocalAssetLocation(file),
                isSynced = false,
                fileName = row.fileName,
                dateInFeed = dateInFeed,
                size = row.size,
                uniqueAssetIdentifier = assetHash,
                duration = row.duration,
                dimensions = dimensions
            )
        )
    }

    private fun <T> querySingleRow(uri: Uri, projection: Array<String>, read: (Cursor) -> T): T? =
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) read(cursor) else null
        }

    private fun queryImages(): Flow<PlatformFile> = flow {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val androidUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val assetPlatformFile = PlatformFile(androidUri)
                emit(assetPlatformFile)
            }
        }
    }.flowOn(ioDispatcher)

    private fun queryVideos(): Flow<PlatformFile> = flow {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/%")
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val androidUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val assetPlatformFile = PlatformFile(androidUri)
                emit(assetPlatformFile)
            }
        }
    }.flowOn(ioDispatcher)

    @OptIn(ExperimentalTime::class)
    private fun Duration.utcMsToLocalDateTime(): LocalDateTime {
        // Convert seconds to milliseconds
        val instant = Instant.fromEpochMilliseconds(this.inWholeMilliseconds)

        // Convert to LocalDateTime in the system's default time zone
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val exifDateTimeFormat = LocalDateTime.Format {
        year(); char(':'); monthNumber(); char(':'); day()
        char(' ')
        hour(); char(':'); minute(); char(':'); second()
    }

    private fun ContentResolver.parseDateTakenFromExif(uri: Uri): LocalDateTime? =
        openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            exifDateString?.let(exifDateTimeFormat::parseOrNull)
        }
}