package com.serratocreations.phovo.feature.photos.ui

import coil3.ImageLoader
import coil3.disk.DiskCache
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.div
import okio.Path.Companion.toPath

actual fun ImageLoader.Builder.platformDiskCache(): ImageLoader.Builder =
    this.diskCache {
        DiskCache.Builder()
            .directory((FileKit.cacheDir / "image_cache").absolutePath().toPath())
            .build()
    }