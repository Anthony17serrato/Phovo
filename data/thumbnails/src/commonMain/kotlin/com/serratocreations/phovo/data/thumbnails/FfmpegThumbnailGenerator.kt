package com.serratocreations.phovo.data.thumbnails

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
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
        outputDirectories: ThumbnailDirectories,
        thumbnailNameWithoutExtension: String
    ): ThumbnailResult = withContext(ioDispatcher) {
        val ffmpegFile = deferredFfmpegFile.await()

        val lowResThumbnail = PlatformFile(
            outputDirectories.lowResThumbnailDirectory,
            "$thumbnailNameWithoutExtension.webp"
        )

        val highResThumbnail = PlatformFile(
            outputDirectories.highResThumbnailDirectory,
            "$thumbnailNameWithoutExtension.webp"
        )

        try {
            if (!ffmpegFile.exists()) {
                error("FFmpeg binary not found at ${ffmpegFile.absolutePath()}")
            }
            // Build FFmpeg command
            val filter =
                "thumbnail,split=2[v1][v2];" +
                        "[v1]scale=320:320:force_original_aspect_ratio=decrease[v1out];" +
                        "[v2]scale=1080:1080:force_original_aspect_ratio=decrease[v2out]"

            val command = listOf(
                ffmpegFile.absolutePath(),
                "-y",
                "-i", videoFile.absolutePath(),
                "-filter_complex", filter,

                // LOW RES (static)
                "-map", "[v1out]",
                "-frames:v", "1",
                "-c:v", "libwebp",
                "-preset", "picture",
                "-q:v", "60",
                "-compression_level", "6",
                lowResThumbnail.absolutePath(),

                // HIGH RES (animated storyboard)
                "-map", "[v2out]",
                "-c:v", "libwebp",
                "-loop", "0",
                "-preset", "picture",
                "-q:v", "75",
                "-compression_level", "6",
                highResThumbnail.absolutePath()
            )

            // Run FFmpeg process
            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()

            val exitCode = process.waitFor()
            if (
                exitCode != 0 ||
                !lowResThumbnail.exists() ||
                !highResThumbnail.exists()
            ) {
                error("FFmpeg failed to generate thumbnails")
            }

            return@withContext ThumbnailResult.Success(highResThumbnail)
        } catch (e: Exception) {
            when(e) {
                is IllegalStateException, is IOException -> {
                    return@withContext ThumbnailResult.Failure
                }
                else -> {
                    // Re-throw unexpected exception
                    throw e
                }
            }
        }
    }
}

sealed interface ThumbnailResult {
    data class Success(val platformFile: PlatformFile): ThumbnailResult
    data object Failure: ThumbnailResult
}