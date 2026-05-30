package com.serratocreations.phovo.feature.photos.ui

import coil3.ImageLoader
import coil3.disk.DiskCache
import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.getPlatform
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import okio.FileSystem
import okio.Path.Companion.toPath

actual fun ImageLoader.Builder.platformDiskCache(): ImageLoader.Builder =
    this.diskCache {
        // TODO Temporary fix ios file .absolutePath issue
        if (getPlatform() == Platform.Ios) {
            DiskCache.Builder().directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(512L * 1024 * 1024) // 512MB
                .build()
        } else {
            val directory = (FileKit.cacheDir / "image_cache")
            directory.createDirectories(mustCreate = false)
            DiskCache.Builder()
                .directory(directory.absolutePath().toPath())
                .build()
        }
    }