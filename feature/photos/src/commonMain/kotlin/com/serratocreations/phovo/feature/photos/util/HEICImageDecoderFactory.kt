package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options

// Decoder logic
class HEICImageDecoderFactory : Decoder.Factory {

    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader,
    ): Decoder {
        return getPlatformHeicDecoder(result.source, options)
    }
}