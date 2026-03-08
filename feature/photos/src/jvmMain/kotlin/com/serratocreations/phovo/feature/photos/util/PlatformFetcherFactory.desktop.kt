package com.serratocreations.phovo.feature.photos.util

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.serratocreations.phovo.data.thumbnails.FfmpegThumbnailGenerator
import com.serratocreations.phovo.data.thumbnails.FfmpegThumbnailResult
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any> = DesktopVideoFrameFetcher.Factory()

class DesktopVideoFrameFetcher(
    private val file: PlatformFile,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val result = FfmpegThumbnailGenerator(Dispatchers.IO, CoroutineScope(SupervisorJob()))
            .generateVideoThumbnail(
                videoFile = file,
                outputDirectory = FileKit.cacheDir
            )
        return@withContext if (result is FfmpegThumbnailResult.Success) {
            val bufferedSource = File(result.platformFile.path).toPath().source().buffer()
            val source = ImageSource(
                source = bufferedSource,
                fileSystem = options.fileSystem
            )

            SourceFetchResult(
                source = source,
                mimeType = "image/png", // Or dynamically determine
                dataSource = DataSource.DISK
            )
        } else {
            null
        }
    }

    class Factory : Fetcher.Factory<Any> {

        override fun create(
            data: Any,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            val file = (data as? Uri)?.path?.let { PlatformFile(it) } ?: return null
            return if (file.extension.lowercase() in listOf("mp4", "mov", "avi", "mkv", "webm")) {
                DesktopVideoFrameFetcher(file, options)
            } else null
        }
    }
}
