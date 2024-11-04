package com.serratocreations.phovo.data.db.dao

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.os.Build
import com.serratocreations.phovo.data.db.entity.PhovoItem
import com.serratocreations.phovo.data.db.entity.PhovoVideoItem
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
        val videoList = mutableListOf<PhovoVideoItem>()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )
        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                videoList += PhovoVideoItem(contentUri, name, duration, size)
            }
        }
        return flowOf(videoList)
    }

    override fun updatePhovoItem(phovoItem: PhovoItem): Boolean {
        TODO("Not yet implemented")
    }
}