package com.serratocreations.phovo.core.domain

import coil3.toUri
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists

actual fun getThumbnailFiles(
    rootOutputDirectory: PlatformFile,
    dataUuid: String
): ThumbnailResources {
        val lowResThumbDir = rootOutputDirectory / "low_res_thumbnails"
        val lowResThumb = PlatformFile(
            base = lowResThumbDir,
            child = "$dataUuid.webp"
        ).let {
            if (it.exists()) it else null
        }
        val highResThumbDir = rootOutputDirectory / "high_res_thumbnails"
        val highResThumb = PlatformFile(
            base = highResThumbDir,
            child = "$dataUuid.webp"
        ).let {
            if (it.exists()) it.absolutePath().toUri() else null
        }
        return ThumbnailResources(
            lowResThumbnailDirectory = lowResThumb,
            highResThumbnail = highResThumb
        )
}