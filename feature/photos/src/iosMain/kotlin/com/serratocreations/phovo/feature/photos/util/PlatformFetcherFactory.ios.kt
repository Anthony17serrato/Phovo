package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.request.Options
import com.serratocreations.phovo.core.common.util.isPhAssetUri

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any> = PhAssetFetcherFactory()

class PhAssetFetcherFactory : Fetcher.Factory<Any> {
    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
        val isPhAssetUri = (data as? Uri)?.isPhAssetUri() ?: false
        return if (isPhAssetUri) {
            PhAssetFetcher(data, options)
        } else null
    }
}