package com.serratocreations.phovo.feature.photos.util

import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.*
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use
import platform.UIKit.UIImage
import platform.Foundation.*
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

class IosHeicImageDecoder(
    source: ImageSource,
    options: Options
): PlatformImageDecoder(source, options) {
    companion object {
        private const val COMPRESSION = 0.8
    }

    override suspend fun decode(): DecodeResult {
        println("IosHeicImageDecoder decode")
        val originalBytes = source.source().use { it.readByteArray() }
        val jpegBytes = originalBytes.toJpegBytes()
        val image = Image.makeFromEncoded(jpegBytes)

        val isSampled: Boolean
        val bitmap: Bitmap
        try {
            bitmap = Bitmap.makeFromImage(image, options)
            bitmap.setImmutable()
            isSampled = bitmap.width < image.width || bitmap.height < image.height
        } finally {
            image.close()
        }

        return DecodeResult(
            image = bitmap.asImage(),
            isSampled = isSampled,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toJpegBytes(): ByteArray = memScoped {
        val image = UIImage(data = this@toJpegBytes.toNSData())

        return UIImageJPEGRepresentation(image, compressionQuality = COMPRESSION)!!.toByteArray()
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = this.size.toULong()
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun NSData.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.length.toInt())
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return byteArray
    }

    /** Create a [Bitmap] from [image] for the given [options]. */
    @OptIn(ExperimentalCoilApi::class)
    internal fun Bitmap.Companion.makeFromImage(
        image: Image,
        options: Options,
    ): Bitmap {
        val srcWidth = image.width
        val srcHeight = image.height
        val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        var multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
        )

        // Only upscale the image if the options require an exact size.
        if (options.precision == Precision.INEXACT) {
            multiplier = multiplier.coerceAtMost(1.0)
        }

        val outWidth = (multiplier * srcWidth).toInt()
        val outHeight = (multiplier * srcHeight).toInt()

        val bitmap = Bitmap()
        bitmap.allocN32Pixels(outWidth, outHeight)
        Canvas(bitmap).use { canvas ->
            canvas.drawImageRect(
                image = image,
                src = Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
                dst = Rect.makeWH(outWidth.toFloat(), outHeight.toFloat()),
            )
        }
        return bitmap
    }
}