package com.serratocreations.phovo.feature.photos.util

import coil3.decode.ImageSource
import coil3.request.Options

actual fun getPlatformHeicDecoder(
    source: ImageSource,
    options: Options
): HeicImageDecoder = IosHeicImageDecoder(source, options)