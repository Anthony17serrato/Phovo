package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.buffer
import okio.source
import phovo.feature.photos.generated.resources.Res
import java.io.File

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any> = DesktopVideoFrameFetcher.Factory()

// TODO: This fetcher is temporary, eventually all incoming image files will be processed by a worker
//  service and at that point thumbnails wil be generated.
class DesktopVideoFrameFetcher(
    private val file: File,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // Create a temp file for extracted frame
        val tempFrameFile = File.createTempFile("frame_", ".png")
        // TODO: Extract OS detection to utility function
        val osName = System.getProperty("os.name").lowercase()
        val execName = if (osName.contains("win")) "ffmpeg.exe" else "ffmpeg"
        val bytes = Res.readBytes("files/$execName")

        val ffmpegFile = File.createTempFile("ffmpeg_", if (execName.endsWith(".exe")) ".exe" else "")
        ffmpegFile.writeBytes(bytes)
        ffmpegFile.setExecutable(true)

        if (!ffmpegFile.exists()) {
            error("FFmpeg binary not found at ${ffmpegFile.absolutePath}")
        }

        try {
            // Build FFmpeg command
            val command = listOf(
                ffmpegFile.absolutePath,
                "-y",  // <-- add this flag here to auto-overwrite
                "-i", file.absolutePath,
                "-ss", "00:00:00.000", // start time, adjust as needed
                "-frames:v", "1",
                "-f", "image2",
                tempFrameFile.absolutePath
            )

            // Run FFmpeg process
            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0 || !tempFrameFile.exists()) {
                error("FFmpeg failed to extract frame")
            }

            // Load the extracted frame PNG file as Coil Image
            val bufferedSource = tempFrameFile.toPath().source().buffer()
            val source = ImageSource(
                source = bufferedSource,
                fileSystem = options.fileSystem
            )

            return SourceFetchResult(
                source = source,
                mimeType = "image/png", // Or dynamically determine
                dataSource = DataSource.DISK
            )
        } finally {
            tempFrameFile.delete()
        }
    }

    class Factory : Fetcher.Factory<Any> {

        override fun create(
            data: Any,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            val file = (data as? Uri)?.path?.let { File(it) } ?: return null
            return if (file.extension.lowercase() in listOf("mp4", "mov", "avi", "mkv", "webm")) {
                DesktopVideoFrameFetcher(file, options)
            } else null
        }
    }
}
