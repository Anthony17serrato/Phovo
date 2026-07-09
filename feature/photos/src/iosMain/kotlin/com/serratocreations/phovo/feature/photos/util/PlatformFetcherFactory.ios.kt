package com.serratocreations.phovo.feature.photos.util

import coil3.fetch.Fetcher
import com.serratocreations.phovo.core.common.util.PhAssetFetcherFactory

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any> = PhAssetFetcherFactory()