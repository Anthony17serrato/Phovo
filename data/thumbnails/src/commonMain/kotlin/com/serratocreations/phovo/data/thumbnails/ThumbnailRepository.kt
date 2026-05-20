package com.serratocreations.phovo.data.thumbnails

import com.serratocreations.phovo.core.common.HIGH_RES_THUMBNAIL_DIR
import com.serratocreations.phovo.core.common.LOW_RES_THUMBNAIL_DIR
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div

class ThumbnailRepository(
    val thumbnailGenerator: FfmpegThumbnailGenerator
) {
    private fun getThumbnailDirectoriesFromRootOutputDirectory(rootOutputDirectory: PlatformFile): ThumbnailDirectories {
        val lowResThumbDir = rootOutputDirectory / LOW_RES_THUMBNAIL_DIR
        lowResThumbDir.createDirectories(mustCreate = false)
        val highResThumbDir = rootOutputDirectory / HIGH_RES_THUMBNAIL_DIR
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
    ) {
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
    ) {
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