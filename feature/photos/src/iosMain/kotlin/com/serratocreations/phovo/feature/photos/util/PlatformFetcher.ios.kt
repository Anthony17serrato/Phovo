package com.serratocreations.phovo.feature.photos.util

import coil3.request.Options

actual fun getPlatformFetcher(
    data: Any,
    options: Options
): PlatformFetcher = PhAssetFetcher(data, options)