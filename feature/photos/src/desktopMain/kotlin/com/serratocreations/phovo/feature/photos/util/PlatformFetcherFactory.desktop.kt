package com.serratocreations.phovo.feature.photos.util

import coil3.fetch.Fetcher

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any> = DefaultPlatformFetcherFactory()