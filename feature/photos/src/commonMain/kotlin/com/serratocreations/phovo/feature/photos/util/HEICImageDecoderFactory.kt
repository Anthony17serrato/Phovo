package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

// Decoder logic
class HEICImageDecoderFactory : Decoder.Factory {

    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader,
    ): Decoder? {
        return if (isHeif(result.source.source())) {
            println("HEICImageDecoderFactory Is Heif")
            getPlatformHeicDecoder(result.source, options)
        } else {
            println("HEICImageDecoderFactory Is Not Heif")
            null
        }
    }

    private val HEIF_HEADER_FTYP = "ftyp".encodeUtf8()
    private val HEIF_BRAND_MIF1 = "mif1".encodeUtf8()
    private val HEIF_BRAND_HEIC = "heic".encodeUtf8()
    private val HEIF_BRAND_HEIX = "heix".encodeUtf8()
    private val HEIF_BRAND_AVIF = "avif".encodeUtf8()
    private val HEIF_BRAND_AVIS = "avis".encodeUtf8()

    /**
     * Return 'true' if the [source] contains a HEIF image. The [source] is not consumed.
     */
    fun isHeif(source: BufferedSource): Boolean {
        // Check if 'ftyp' exists at offset 4
        return source.rangeEquals(4, HEIF_HEADER_FTYP) &&
                (source.rangeEquals(8, HEIF_BRAND_MIF1) || // Single image HEIF
                        source.rangeEquals(8, HEIF_BRAND_HEIC) || // HEIF with HEVC codec
                        source.rangeEquals(8, HEIF_BRAND_HEIX) || // HEIF extension
                        source.rangeEquals(8, HEIF_BRAND_AVIF) || // HEIF with AV1 codec
                        source.rangeEquals(8, HEIF_BRAND_AVIS))   // AV1 Image Sequence
    }
}