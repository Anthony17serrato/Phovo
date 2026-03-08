package com.serratocreations.phovo.data.thumbnails

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div

class ThumbnailRepository(
    val thumbnailGenerator: FfmpegThumbnailGenerator
) {
    private fun getThumbnailDirectoryFromRootOutputDirectory(rootOutputDirectory: PlatformFile): PlatformFile {
        val thumbDir = rootOutputDirectory / "thumbnails"
        thumbDir.createDirectories(mustCreate = false)
        return thumbDir
    }

    suspend fun generateVideoThumbnail(
        rootOutputDirectory: PlatformFile,
        videoFile: PlatformFile
    ) {
        thumbnailGenerator.generateVideoThumbnail(
            videoFile = videoFile,
            outputDirectory = getThumbnailDirectoryFromRootOutputDirectory(rootOutputDirectory)
        )
    }
}