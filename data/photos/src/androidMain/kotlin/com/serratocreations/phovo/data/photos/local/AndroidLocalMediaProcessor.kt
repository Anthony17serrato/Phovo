package com.serratocreations.phovo.data.photos.local

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.serratocreations.phovo.core.common.HIGH_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.extension.androidUri
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AndroidLocalMediaProcessor(
    private val ioDispatcher: CoroutineDispatcher,
    private val fileHashCalculator: FileHashCalculator,
    private val context: Context,
    private val imageLoader: coil3.ImageLoader
) : LocalMediaProcessor {
    private val resolver = context.contentResolver

    override fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        processMediaChannel: SendChannel<MediaItem>
    ) = launch {
        // TODO add observability of updates(Probably by registering Broadcast receiver)
        val (processedVideos, processedImages) = processedItems.segregate()
        queryImages(processedImages)
            .onEach { processedImage ->
                processMediaChannel.send(processedImage)
            }.launchIn(this)

        queryVideos(processedVideos)
            .onEach { processedVideo ->
                processMediaChannel.send(processedVideo)
            }.launchIn(this)
    }

    private fun queryImages(
        alreadyProcessedImages: List<MediaImageItem>
    ): Flow<MediaItem> = flow {
        val processedImageHashes = alreadyProcessedImages.map { it.uniqueAssetIdentifier }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val fileName = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn).toLong()
                val androidUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val assetPlatformFile = PlatformFile(androidUri)
                val assetHash = fileHashCalculator.computeSha256(assetPlatformFile)
                val assetLocation = AssetLocation.LocalAssetLocation(assetPlatformFile)
                // Check if media has already been processed
                if (assetHash in processedImageHashes) {
                    // TODO if hash exists make sure the asset location has not changed, if it changed
                    //  it is needed to update
                    continue
                }
                createLowResThumbnail(assetPlatformFile, assetHash)
                createHighResThumbnail(assetPlatformFile, assetHash)
                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: resolver.parseDateTakenFromExif(androidUri)
                    ?: (cursor.getLong(dateAddedColumn) * 1000).utcMsToLocalDateTime()

                val mediaImageItem = MediaImageItem(
                    assetLocation = assetLocation,
                    isSynced = false,
                    fileName = fileName,
                    dateInFeed = dateInFeed,
                    size = size,
                    uniqueAssetIdentifier = assetHash
                )
                emit(mediaImageItem)
            }
        }
    }.flowOn(ioDispatcher)

    private fun queryVideos(
        alreadyProcessedVideos: List<MediaVideoItem>
    ): Flow<MediaItem> = flow {
        val processedVideoHashes = alreadyProcessedVideos.map { it.uniqueAssetIdentifier }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DURATION
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/%")
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn).toLong()
                val duration = cursor.getLong(durationColumn).milliseconds
                val androidUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val assetPlatformFile = PlatformFile(androidUri)
                val assetHash = fileHashCalculator.computeSha256(assetPlatformFile)
                val assetLocation = AssetLocation.LocalAssetLocation(assetPlatformFile)
                // Check if media has already been processed
                if (assetHash in processedVideoHashes) {
                    // TODO if hash exists make sure the asset location has not changed, if it changed
                    //  it is needed to update
                    continue
                }

                createLowResThumbnail(assetPlatformFile, assetHash)
                createHighResThumbnail(assetPlatformFile, assetHash)

                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: (cursor.getLong(dateAddedColumn) * 1000).utcMsToLocalDateTime()

                val mediaVideoItem = MediaVideoItem(
                    assetLocation = assetLocation,
                    isSynced = false,
                    fileName = name,
                    dateInFeed = dateInFeed,
                    size = size,
                    uniqueAssetIdentifier = assetHash,
                    duration = duration
                )
                emit(mediaVideoItem)
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun createLowResThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String
    ) {
        generateThumbnail(
            originalImageFile = originalImageFile,
            assetHash = assetHash,
            size = 32,
            quality = 60,
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
            size = 512,
            quality = 60,
            targetDirName = HIGH_RES_THUMBNAIL_DIR
        )
    }

    private suspend fun generateThumbnail(
        originalImageFile: PlatformFile,
        assetHash: String,
        size: Int,
        quality: Int,
        targetDirName: String
    ): Unit = withContext(ioDispatcher) {
        try {
            val thumbnailDir = FileKit.filesDir / targetDirName
            val thumbnailFile = PlatformFile(thumbnailDir, "$assetHash.jpg")

            if (thumbnailFile.exists()) {
                return@withContext
            }

            val androidUri = originalImageFile.androidFile.androidUri

            val request = ImageRequest.Builder(context)
                .data(androidUri)
                .size(size)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
                val compressedBytes = outputStream.toByteArray()

                thumbnailDir.createDirectories(mustCreate = false)
                thumbnailFile write compressedBytes
            }
        } catch (e: Exception) {
            PhovoLogger.withTag("AndroidLocalMediaProcessor").e(throwable = e) {
                "generateThumbnail Failed for $originalImageFile (size=$size): ${e.message}"
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun Long.utcMsToLocalDateTime(): LocalDateTime {
        // Convert seconds to milliseconds
        val instant = Instant.fromEpochMilliseconds(this)

        // Convert to LocalDateTime in the system's default time zone
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    // TODO Address lint
    @SuppressLint("NewApi")
    private fun ContentResolver.parseDateTakenFromExif(uri: Uri) : LocalDateTime? =
        openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val exifDateFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            var exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            if (exifDateString == null) {
                exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME)
            }
            if (exifDateString != null) {
                return@use java.time.LocalDateTime.parse(exifDateString, exifDateFormatter).toKotlinLocalDateTime()
            } else return@use null
        }
}