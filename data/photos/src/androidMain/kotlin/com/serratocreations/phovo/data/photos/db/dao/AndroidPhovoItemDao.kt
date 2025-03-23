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
import android.util.Log
import androidx.core.database.getLongOrNull
import coil3.toCoilUri
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import java.time.LocalDateTime as JavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.koin.core.annotation.Singleton

// https://github.com/InsertKoinIO/koin-annotations/issues/249
@Singleton(binds = [PhovoItemDao::class])
class AndroidPhovoItemDao(
    context: Context
) : PhovoItemDao {
    private val resolver = context.contentResolver

    @SuppressLint("NewApi")
    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        // TODO add observability of updates
        val videoList = mutableListOf<PhovoImageItem>()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM%")

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val query = resolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn)
                val androidUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val contentUri = androidUri.toCoilUri()
                val dateInFeed = cursor.getLongOrNull(dateTakenColumn)?.utcMsToLocalDateTime()
                    ?: run {
                        Log.w("AndroidPhovoItemDao", "Could not get date taken for $name")
                        // try to parse the date from exif data, if all fails fallback to date added.
                        resolver.parseDateTakenFromExif(androidUri)
                            ?: (cursor.getLong(dateAddedColumn)*1000).utcMsToLocalDateTime()
                    }

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                videoList += PhovoImageItem(
                    uri = contentUri,
                    name = name,
                    dateInFeed = dateInFeed,
                    size = size
                )
            }
        }
        return flowOf(videoList)
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