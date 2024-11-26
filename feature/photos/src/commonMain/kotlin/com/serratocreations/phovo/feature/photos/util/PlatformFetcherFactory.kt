package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.request.Options

expect fun getPlatformFetcherFactory(): Fetcher.Factory<Any>

class DefaultPlatformFetcherFactory : Fetcher.Factory<Any> {
    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
        // No-op default fetcher factory(delegates to another fetcher)
        return null
    }
}