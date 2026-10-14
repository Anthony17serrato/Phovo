package com.serratocreations.phovo.data.photos.local

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.serratocreations.phovo.core.model.MediaDimensions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads the size media is displayed at from the file itself. Safe to call from any thread. */
internal class AndroidMediaDimensionsReader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Reads the size of the image at [uri] from its header and its EXIF orientation, without
     * decoding the pixels. Returns null when the size cannot be read or the orientation is out of
     * range, which fails processing for the image.
     */
    suspend fun readImageDimensions(uri: Uri): MediaDimensions? = withContext(ioDispatcher) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            // EXIF defines a missing orientation as normal.
            val orientation = resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            // A header that cannot be read leaves the bounds at -1.
            val isValid = bounds.outWidth > 0 && bounds.outHeight > 0 &&
                orientation in MediaDimensions.EXIF_ORIENTATIONS
            if (isValid) {
                MediaDimensions(bounds.outWidth, bounds.outHeight).withExifOrientation(orientation)
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Reads the size and rotation of the video at [uri]. Returns null when either is missing or out
     * of range, which fails processing for the video.
     */
    suspend fun readVideoDimensions(uri: Uri): MediaDimensions? = withContext(ioDispatcher) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val width = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            // Videos without a rotation are not rotated.
            val rotation = retriever.intMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: 0
            if (width != null && height != null && width > 0 && height > 0 && rotation % 90 == 0) {
                MediaDimensions(width, height).rotatedBy(rotation)
            } else {
                null
            }
        } catch (e: RuntimeException) {
            // setDataSource throws IllegalArgumentException or RuntimeException for unreadable files.
            null
        } finally {
            retriever.release()
        }
    }

    private fun MediaMetadataRetriever.intMetadata(key: Int): Int? = extractMetadata(key)?.toIntOrNull()
}
