package com.serratocreations.phovo.data.photos.db.dao

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
// TODO follow suggestion
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.os.Build
import androidx.core.database.getLongOrNull
import coil3.toCoilUri
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoVideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import java.time.LocalDateTime as JavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlin.time.Duration.Companion.milliseconds

class AndroidPhovoItemDao(
    context: Context
) : PhovoItemDao {
    private val resolver = context.contentResolver

    @SuppressLint("NewApi")
    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        // TODO add observability of updates
        val images = queryImages()
        val videos = queryVideos()
        val allItems = (images + videos).sortedByDescending { it.dateInFeed }

        return flowOf(allItems)
    }

    private fun queryImages(): List<PhovoItem> {
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

        val result = mutableListOf<PhovoItem>()
        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn)
                val androidUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val contentUri = androidUri.toCoilUri()

                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: resolver.parseDateTakenFromExif(androidUri)
                    ?: (cursor.getLong(dateAddedColumn) * 1000).utcMsToLocalDateTime()

                result += PhovoImageItem(
                    uri = contentUri,
                    name = name,
                    dateInFeed = dateInFeed,
                    size = size
                )
            }
        }
        return result
    }

    private fun queryVideos(): List<PhovoItem> {
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

        val result = mutableListOf<PhovoItem>()
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
                val size = cursor.getInt(sizeColumn)
                val duration = cursor.getLong(durationColumn).milliseconds
                val androidUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val contentUri = androidUri.toCoilUri()

                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: (cursor.getLong(dateAddedColumn) * 1000).utcMsToLocalDateTime()

                result += PhovoVideoItem(
                    uri = contentUri,
                    name = name,
                    dateInFeed = dateInFeed,
                    size = size,
                    duration = duration
                )
            }
        }
        return result
    }

    private fun Long.utcMsToLocalDateTime(): KotlinLocalDateTime {
        // Convert seconds to milliseconds
        val instant = Instant.fromEpochMilliseconds(this)

        // Convert to LocalDateTime in the system's default time zone
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    // TODO Address lint
    @SuppressLint("NewApi")
    private fun ContentResolver.parseDateTakenFromExif(uri: Uri) : KotlinLocalDateTime? =
        openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val exifDateFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            var exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            if (exifDateString == null) {
                exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME)
            }
            if (exifDateString != null) {
                return@use JavaLocalDateTime.parse(exifDateString, exifDateFormatter).toKotlinLocalDateTime()
            } else return@use null
        }
}