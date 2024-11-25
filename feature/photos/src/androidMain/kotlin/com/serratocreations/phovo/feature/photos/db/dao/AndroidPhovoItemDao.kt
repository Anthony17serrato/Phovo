package com.serratocreations.phovo.feature.photos.db.dao

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
// TODO follow warning guidance
import android.media.ExifInterface
import coil3.Uri
import android.provider.MediaStore
import android.os.Build
import coil3.toCoilUri
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoImageItem
import com.serratocreations.phovo.feature.photos.data.db.dao.PhovoItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime as JavaLocalDateTime
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import java.time.format.DateTimeFormatter

class AndroidPhovoItemDao(
    context: Context
) : PhovoItemDao {
    private val resolver = context.contentResolver

    override fun addItem(phovoItem: PhovoItem) {
        TODO("Not yet implemented")
    }

    @SuppressLint("NewApi")
    override fun allItemsFlow(): Flow<List<PhovoItem>> {
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
            MediaStore.Images.Media.DATE_TAKEN
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/Camera%")

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

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn)
                val androidUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val contentUri: Uri = androidUri.toCoilUri()

                // read file metadata
                var exifDate: KotlinLocalDateTime? = null
                resolver.openInputStream(androidUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val exifDateFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                    var exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    if (exifDateString == null) {
                        exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    }
                    if (exifDateString != null) {
                        exifDate = JavaLocalDateTime.parse(exifDateString, exifDateFormatter).toKotlinLocalDateTime()
                    }
                }

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                videoList += PhovoImageItem(
                    uri = contentUri,
                    name = name,
                    dateTaken = exifDate,
                    size = size
                )
            }
        }
        return flowOf(videoList)
    }

    override fun updatePhovoItem(phovoItem: PhovoItem): Boolean {
        TODO("Not yet implemented")
    }
}