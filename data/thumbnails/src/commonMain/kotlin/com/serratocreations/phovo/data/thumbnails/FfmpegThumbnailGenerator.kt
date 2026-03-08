package com.serratocreations.phovo.data.thumbnails

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.utils.Platform
import io.github.vinceglb.filekit.utils.PlatformUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import phovo.data.thumbnails.generated.resources.Res
import java.io.File
import java.io.IOException
import java.net.URI

class FfmpegThumbnailGenerator(
    private val ioDispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope
) {
    // TODO Can move to constructor DI if other classes need it
    val deferredFfmpegFile: Deferred<PlatformFile> by lazy {
        return@lazy appScope.async {
            val execName = if (PlatformUtil.current == Platform.Windows) "ffmpeg.exe" else "ffmpeg"

            val ffmpegFile = File.createTempFile("ffmpeg_", if (execName.endsWith(".exe")) ".exe" else "")
            URI(Res.getUri("files/$execName")).toURL().openStream().use { inputStream ->
                ffmpegFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            // FileKit does not support this API
            ffmpegFile.setExecutable(true)
            return@async PlatformFile(ffmpegFile)
        }
    }

    /**
     * Given an input [videoFile] generates
     */
    suspend fun generateVideoThumbnail(
        videoFile: PlatformFile,
        outputDirectory: PlatformFile,
        thumbnailNameWithoutExtension: String
    ): FfmpegThumbnailResult = withContext(ioDispatcher) {
        val ffmpegFile = deferredFfmpegFile.await()
        // Creates the directory if it does not already exist
        outputDirectory.createDirectories(mustCreate = false)

        // Create a temp file for extracted frame
        val outputThumbnail = PlatformFile(outputDirectory, "$thumbnailNameWithoutExtension.webp")

        try {
            if (!ffmpegFile.exists()) {
                error("FFmpeg binary not found at ${ffmpegFile.absolutePath()}")
            }
            // Build FFmpeg command
            val command = listOf(
                ffmpegFile.absolutePath(),
                "-y",
                "-i", videoFile.absolutePath(),
                "-frames:v", "1",
                "-vf", "thumbnail,scale=320:320:force_original_aspect_ratio=decrease",
                "-c:v", "libwebp",
                "-q:v", "60",
                "-compression_level", "6",
                outputThumbnail.absolutePath()
            )

            // Run FFmpeg process
            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0 || !outputThumbnail.exists()) {
                error("FFmpeg failed to extract frame")
            }

            return@withContext FfmpegThumbnailResult.Success(outputThumbnail)
        } catch (e: Exception) {
            when(e) {
                is IllegalStateException, is IOException -> {
                    return@withContext FfmpegThumbnailResult.Failure
                }
                else -> {
                    // Re-throw unexpected exception
                    throw e
                }
            }
        }
    }
}

sealed interface FfmpegThumbnailResult {
    data class Success(val platformFile: PlatformFile): FfmpegThumbnailResult
    data object Failure: FfmpegThumbnailResult
}