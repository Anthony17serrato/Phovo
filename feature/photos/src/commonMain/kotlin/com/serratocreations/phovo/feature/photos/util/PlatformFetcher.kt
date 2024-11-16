package com.serratocreations.phovo.feature.photos.util

import coil3.fetch.Fetcher
import coil3.request.Options

abstract class PlatformFetcher(
    val data: Any,
    val options: Options
): Fetcher

expect fun getPlatformFetcher(data: Any, options: Options): PlatformFetcher