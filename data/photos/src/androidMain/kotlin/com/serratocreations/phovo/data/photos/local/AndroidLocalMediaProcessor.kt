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
import coil3.toCoilUri
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.repository.util.segregate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

class AndroidLocalMediaProcessor(
    private val ioDispatcher: CoroutineDispatcher,
    context: Context
) : LocalMediaProcessor {
    private val resolver = context.contentResolver

    override fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        localDirectory: String?,
        processMediaChannel: SendChannel<MediaItem>
    ) = launch {
        // TODO add observability of updates(Probably by registering Broadcast receiver)
        val (processedVideos, processedImages) = processedItems.segregate()
        queryImages(processedImages)
            .onEach { processedImage ->
                processMediaChannel.send(processedImage)
            }.flowOn(ioDispatcher)
            .launchIn(this)
        queryVideos(processedVideos)
            .onEach { processedVideo ->
                processMediaChannel.send(processedVideo)
            }.flowOn(ioDispatcher)
            .launchIn(this)
    }

    private fun queryImages(
        alreadyProcessedImages: List<MediaImageItem>
    ): Flow<MediaItem> = flow {
        val processedImageIds = alreadyProcessedImages.map { it.fileName }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
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
                if (fileName in processedImageIds) continue
                val size = cursor.getInt(sizeColumn)
                val androidUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val contentUri = androidUri.toCoilUri()

                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: resolver.parseDateTakenFromExif(androidUri)
                    ?: (cursor.getLong(dateAddedColumn) * 1000).utcMsToLocalDateTime()

                emit(MediaImageItem(
                    uri = contentUri,
                    fileName = fileName,
                    dateInFeed = dateInFeed,
                    size = size
                ))
            }
        }
    }

    private fun queryVideos(
        alreadyProcessedVideos: List<MediaVideoItem>
    ): Flow<MediaItem> = flow {
        val processedVideoIds = alreadyProcessedVideos.map { it.fileName }
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
                if (name in processedVideoIds) continue
                val size = cursor.getInt(sizeColumn)
                val duration = cursor.getLong(durationColumn).milliseconds
                val androidUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val contentUri = androidUri.toCoilUri()

                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: (cursor.getLong(dateAddedColumn) * 1000).utcMsToLocalDateTime()

                emit(MediaVideoItem(
                    uri = contentUri,
                    fileName = name,
                    dateInFeed = dateInFeed,
                    size = size,
                    duration = duration
                ))
            }
        }
    }

    private fun Long.utcMsToLocalDateTime(): LocalDateTime {
        // Convert seconds to milliseconds
        val instant = Instant.Companion.fromEpochMilliseconds(this)

        // Convert to LocalDateTime in the system's default time zone
        return instant.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
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