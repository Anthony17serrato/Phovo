package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.request.Options

class PlatformFetcherFactory : Fetcher.Factory<Any> {
    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
        val canFetchData = (data as? Uri)?.toString()?.startsWith("phasset://") ?: false
        println("PlatformFetcherFactory canFetchData $canFetchData $data")
        return if (canFetchData) {
            getPlatformFetcher(data, options)
        } else null
    }
}