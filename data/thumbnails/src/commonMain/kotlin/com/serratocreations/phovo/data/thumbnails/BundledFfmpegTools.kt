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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The FFmpeg executables bundled as Compose resources, see ProvisionFfmpegTask. A resource cannot
 * be executed in place, so each one is installed to `<installDirectory>/<tool>/<checksum>/` the first
 * time it is needed, and reused from there on later launches. The checksum comes from the build, so
 * a new FFmpeg version installs to a new folder, and the folders of older versions are deleted.
 */
class BundledFfmpegTools(
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val installDirectory: File
) {
    val ffmpeg: Deferred<PlatformFile> by lazy { install("ffmpeg") }

    val ffprobe: Deferred<PlatformFile> by lazy { install("ffprobe") }

    private fun install(tool: String): Deferred<PlatformFile> = appScope.async(ioDispatcher) {
        val suffix = if (PlatformUtil.current == Platform.Windows) ".exe" else ""
        val executableName = "$tool$suffix"
        val checksum = Res.readBytes("files/$executableName.sha256").decodeToString().trim()
        check(checksum.matches(SHA_256)) { "Bundled checksum for $executableName is malformed" }

        val toolDirectory = File(installDirectory, tool)
        val versionDirectory = File(toolDirectory, checksum)
        val executable = File(versionDirectory, executableName)
        if (!executable.isFile) {
            versionDirectory.mkdirs()
            // Copied under a temporary name and moved into place, so a launch that dies part way
            // through cannot leave a truncated executable for the next launch to reuse.
            val partial = File.createTempFile("$executableName.", ".part", versionDirectory)
            try {
                URI(Res.getUri("files/$executableName")).toURL().openStream().use { input ->
                    partial.outputStream().use { output -> input.copyTo(output) }
                }
                // FileKit does not support this API
                partial.setExecutable(true)
                moveIntoPlace(partial, executable)
            } finally {
                partial.delete()
            }
        }

        toolDirectory.listFiles { file -> file.isDirectory && file.name != checksum }
            ?.forEach { it.deleteRecursively() }
        PlatformFile(executable)
    }

    private fun moveIntoPlace(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        val SHA_256 = Regex("[0-9a-f]{64}")
    }
}
