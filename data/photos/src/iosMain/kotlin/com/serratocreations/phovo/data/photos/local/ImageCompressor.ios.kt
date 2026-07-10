package com.serratocreations.phovo.data.photos.local

import coil3.Image
import coil3.toBitmap
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import okio.Path.Companion.toPath
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.EncodedImageFormat

actual fun PlatformFile.toCoilData(): Any {
    return this.absolutePath().toPath()
}

actual fun Image.compressToJpeg(quality: Int): ByteArray {
    val bitmap = this.toBitmap()
    val skiaImage = SkiaImage.makeFromBitmap(bitmap)
    val data = skiaImage.encodeToData(EncodedImageFormat.JPEG, quality)
    return data?.bytes ?: throw IllegalStateException("Failed to encode Skia image")
}
