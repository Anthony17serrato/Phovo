package com.serratocreations.phovo.data.photos.local

import coil3.Image
import coil3.toBitmap
import java.io.ByteArrayOutputStream
import android.os.Build

actual fun Image.compressToWebp(quality: Int): ByteArray {
    val bitmap = this.toBitmap()
    val outputStream = ByteArrayOutputStream()
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        android.graphics.Bitmap.CompressFormat.WEBP
    }
    bitmap.compress(format, quality, outputStream)
    return outputStream.toByteArray()
}
