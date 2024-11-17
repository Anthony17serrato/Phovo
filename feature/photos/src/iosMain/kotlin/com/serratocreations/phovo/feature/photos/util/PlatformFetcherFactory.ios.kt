package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.request.Options

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any> = PhAssetFetcherFactory()

class PhAssetFetcherFactory : Fetcher.Factory<Any> {
    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
        val canFetchData = (data as? Uri)?.toString()?.startsWith("phasset://") ?: false
        println("PlatformFetcherFactory canFetchData $canFetchData $data")
        return if (canFetchData) {
            PhAssetFetcher(data, options)
        } else null
    }
}