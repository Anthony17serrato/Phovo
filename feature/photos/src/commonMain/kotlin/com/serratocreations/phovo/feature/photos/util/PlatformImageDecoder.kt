package com.serratocreations.phovo.feature.photos.util

import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.request.Options

abstract class PlatformImageDecoder(
    val source: ImageSource,
    val options: Options
) : Decoder