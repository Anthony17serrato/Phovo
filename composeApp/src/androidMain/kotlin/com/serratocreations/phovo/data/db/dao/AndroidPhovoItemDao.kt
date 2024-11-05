package com.serratocreations.phovo.data.db.dao

import android.content.ContentUris
import android.content.Context
import coil3.Uri
import android.provider.MediaStore
import android.os.Build
import coil3.toCoilUri
import com.serratocreations.phovo.data.db.entity.PhovoItem
import com.serratocreations.phovo.data.db.entity.PhovoImageItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AndroidPhovoItemDao(
    private val context: Context
) : PhovoItemDao {
    override fun addItem(phovoItem: PhovoItem) {
        TODO("Not yet implemented")
    }

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
            MediaStore.Images.Media.SIZE
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/Camera%")

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

        val query = context.contentResolver.query(
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

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toCoilUri()

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                videoList += PhovoImageItem(contentUri, name, size)
            }
        }
        return flowOf(videoList)
    }

    override fun updatePhovoItem(phovoItem: PhovoItem): Boolean {
        TODO("Not yet implemented")
    }
}