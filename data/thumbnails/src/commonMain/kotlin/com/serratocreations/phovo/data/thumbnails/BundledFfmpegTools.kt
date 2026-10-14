package com.serratocreations.phovo.data.thumbnails

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.utils.Platform
import io.github.vinceglb.filekit.utils.PlatformUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import phovo.data.thumbnails.generated.resources.Res
import java.io.File
import java.net.URI

/**
 * The FFmpeg executables bundled as Compose resources, see ProvisionFfmpegTask. A resource cannot
 * be executed in place, so each one is copied out to a temporary file the first time it is needed.
 */
class BundledFfmpegTools(
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) {
    val ffmpeg: Deferred<PlatformFile> by lazy { extract("ffmpeg") }

    val ffprobe: Deferred<PlatformFile> by lazy { extract("ffprobe") }

    private fun extract(tool: String): Deferred<PlatformFile> = appScope.async(ioDispatcher) {
        val suffix = if (PlatformUtil.current == Platform.Windows) ".exe" else ""
        val toolFile = File.createTempFile("${tool}_", suffix)
        URI(Res.getUri("files/$tool$suffix")).toURL().openStream().use { inputStream ->
            toolFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        // FileKit does not support this API
        toolFile.setExecutable(true)
        PlatformFile(toolFile)
    }
}
