package com.serratocreations.phovo.data.photos.local

import coil3.Image
import coil3.toBitmap
import com.serratocreations.phovo.core.common.extension.androidUri
import io.github.vinceglb.filekit.PlatformFile
import java.io.ByteArrayOutputStream

actual fun PlatformFile.toCoilData(): Any {
    return this.androidFile.androidUri
}

actual fun Image.compressToJpeg(quality: Int): ByteArray {
    val bitmap = this.toBitmap()
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}
