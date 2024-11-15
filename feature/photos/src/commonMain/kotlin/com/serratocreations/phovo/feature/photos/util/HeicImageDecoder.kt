package com.serratocreations.phovo.feature.photos.util

import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.request.Options

abstract class HeicImageDecoder(
    val source: ImageSource,
    val options: Options
) : Decoder

expect fun getPlatformHeicDecoder(source: ImageSource, options: Options) : HeicImageDecoder
