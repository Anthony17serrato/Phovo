package com.serratocreations.phovo.data.thumbnails

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div

class ThumbnailRepository(
    val thumbnailGenerator: FfmpegThumbnailGenerator
) {
    private fun getThumbnailDirectoriesFromRootOutputDirectory(rootOutputDirectory: PlatformFile): ThumbnailDirectories {
        val lowResThumbDir = rootOutputDirectory / "low_res_thumbnails"
        lowResThumbDir.createDirectories(mustCreate = false)
        val highResThumbDir = rootOutputDirectory / "high_res_thumbnails"
        highResThumbDir.createDirectories(mustCreate = false)
        return ThumbnailDirectories(
            lowResThumbnailDirectory = lowResThumbDir,
            highResThumbnailDirectory = highResThumbDir
        )
    }

    suspend fun generateVideoThumbnails(
        rootOutputDirectory: PlatformFile,
        videoFile: PlatformFile,
        thumbnailName: String
    ): ThumbnailResult {
        return thumbnailGenerator.generateVideoThumbnail(
            videoFile = videoFile,
            outputDirectories = getThumbnailDirectoriesFromRootOutputDirectory(rootOutputDirectory),
            thumbnailNameWithoutExtension = thumbnailName
        )
    }

    suspend fun generateImageThumbnails(
        rootOutputDirectory: PlatformFile,
        imageFile: PlatformFile,
        thumbnailName: String
    ): ThumbnailResult {
        return thumbnailGenerator.generateImageThumbnail(
            imageFile = imageFile,
            outputDirectories = getThumbnailDirectoriesFromRootOutputDirectory(rootOutputDirectory),
            thumbnailNameWithoutExtension = thumbnailName
        )
    }
}

data class ThumbnailDirectories(
    val lowResThumbnailDirectory: PlatformFile,
    val highResThumbnailDirectory: PlatformFile
)