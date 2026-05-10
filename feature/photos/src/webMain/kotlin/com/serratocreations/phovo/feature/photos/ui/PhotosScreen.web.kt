package com.serratocreations.phovo.feature.photos.ui

import coil3.ImageLoader
import coil3.disk.DiskCache
import okio.FileSystem

actual fun ImageLoader.Builder.platformDiskCache(): ImageLoader.Builder =
    this.diskCache {
        DiskCache.Builder().directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
            .maxSizeBytes(512L * 1024 * 1024) // 512MB
            .build()
    }