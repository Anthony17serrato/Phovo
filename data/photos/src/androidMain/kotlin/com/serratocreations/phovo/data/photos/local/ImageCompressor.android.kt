package com.serratocreations.phovo.data.photos.local

import coil3.Image
import coil3.toBitmap
import java.io.ByteArrayOutputStream

actual fun Image.compressToJpeg(quality: Int): ByteArray {
    val bitmap = this.toBitmap()
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}
