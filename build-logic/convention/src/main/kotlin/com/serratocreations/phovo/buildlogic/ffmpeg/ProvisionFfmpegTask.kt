package com.serratocreations.phovo.buildlogic.ffmpeg

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.zip.ZipFile

/**
 * Downloads the pinned FFmpeg build for the host platform, verifies it against its SHA-256, and
 * unpacks the executable into `<outputDirectory>/files/` so Compose can pick it up as a custom
 * resource directory.
 *
 * Replaces the previous Git LFS checkout of roughly half a gigabyte of committed binaries.
 *
 * Archives are cached under the Gradle user home rather than the build directory, so `clean` does
 * not force another download. The cache is content addressed by checksum, which means bumping the
 * pinned version can never collide with a previously cached archive of the same file name.
 */
@DisableCachingByDefault(
    because = "Output is a single large binary that is cheaper to keep in the local archive cache " +
        "than to move through the build cache"
)
abstract class ProvisionFfmpegTask : DefaultTask() {

    @get:Input
    abstract val downloadUrl: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:Input
    abstract val archiveFormat: Property<FfmpegArchiveFormat>

    /** `ffmpeg`, or `ffmpeg.exe` on Windows. */
    @get:Input
    abstract val executableName: Property<String>

    /** Named only for error messages, so a failure says which target it was provisioning. */
    @get:Input
    abstract val platform: Property<String>

    /**
     * Shared across builds and across `clean`, so this is deliberately not tracked as an input or
     * an output: its contents are addressed by the checksum that is already an input.
     */
    @get:Internal
    abstract val archiveCacheDirectory: DirectoryProperty

    @get:Internal
    abstract val offline: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun provision() {
        val expectedSha = sha256.get().lowercase()
        val archive = resolveArchive(expectedSha)

        val filesDir = outputDirectory.get().asFile.resolve("files")
        // Wipe first so a stale binary from another platform cannot survive here.
        filesDir.deleteRecursively()
        filesDir.mkdirs()

        val target = filesDir.resolve(executableName.get())
        extractExecutable(archive, target)
        target.setExecutable(true, false)

        logger.lifecycle("Provisioned FFmpeg ${FfmpegBinaries.VERSION_BRANCH} (${platform.get()}) at $target")
    }

    /**
     * Returns a verified archive, downloading it only if the cache does not already hold one whose
     * contents hash to [expectedSha].
     */
    private fun resolveArchive(expectedSha: String): File {
        val cacheDir = archiveCacheDirectory.get().asFile
        cacheDir.mkdirs()

        val fileName = downloadUrl.get().substringAfterLast('/')
        val cached = cacheDir.resolve("$expectedSha-$fileName")

        // A cached archive is named for its own checksum, but rehash it anyway: a half written file
        // from an interrupted build would otherwise be handed to the extractor.
        if (cached.isFile && cached.sha256() == expectedSha) {
            logger.info("Using cached FFmpeg archive $cached")
            return cached
        }
        cached.delete()

        if (offline.getOrElse(false)) {
            throw GradleException(
                "FFmpeg archive for ${platform.get()} is not in the cache at $cacheDir and Gradle " +
                    "is running with --offline. Re-run without --offline to download it from " +
                    "${downloadUrl.get()}."
            )
        }

        download(cached, expectedSha)
        return cached
    }

    private fun download(target: File, expectedSha: String) {
        val url = downloadUrl.get()
        val temp = File(target.parentFile, "${target.name}.part")
        var lastFailure: Exception? = null

        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            temp.delete()
            try {
                logger.lifecycle(
                    "Downloading FFmpeg ${FfmpegBinaries.VERSION_BRANCH} for ${platform.get()} from $url"
                )
                fetch(url, temp)

                val actualSha = temp.sha256()
                if (actualSha == expectedSha) {
                    temp.renameTo(target)
                    return
                }

                lastFailure = GradleException(checksumMismatchMessage(url, expectedSha, actualSha, temp.length()))
            } catch (e: Exception) {
                lastFailure = e
            }

            if (attempt < DOWNLOAD_ATTEMPTS - 1) {
                logger.warn("FFmpeg download attempt ${attempt + 1} failed (${lastFailure?.message}), retrying")
                Thread.sleep(RETRY_BACKOFF_MILLIS * (attempt + 1))
            }
        }

        temp.delete()
        throw GradleException(
            "Failed to download a verified FFmpeg build for ${platform.get()} from $url after " +
                "$DOWNLOAD_ATTEMPTS attempts.",
            lastFailure
        )
    }

    private fun fetch(url: String, target: File) {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build()

        val request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofMinutes(REQUEST_TIMEOUT_MINUTES))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(target.toPath()))
        if (response.statusCode() !in 200..299) {
            throw GradleException("$url returned HTTP ${response.statusCode()}")
        }
    }

    /**
     * The macOS arm64 host answers with an HTML proof of work challenge when it decides a client
     * looks automated, so a mismatch here is far more likely to be a challenge page than a corrupted
     * download. Say so, otherwise the failure reads as an unexplained checksum error.
     */
    private fun checksumMismatchMessage(
        url: String,
        expected: String,
        actual: String,
        size: Long
    ): String = buildString {
        append("Checksum mismatch for $url\n")
        append("  expected: $expected\n")
        append("  actual:   $actual\n")
        append("  size:     $size bytes\n")
        append(
            "The response was not the expected archive. This is usually a bot challenge or error " +
                "page served in place of the download, or a source that republished the file under " +
                "the same URL. Verify the download by hand before changing the pinned checksum."
        )
    }

    private fun extractExecutable(archive: File, target: File) {
        val executable = executableName.get()
        val extracted = when (archiveFormat.get()) {
            FfmpegArchiveFormat.ZIP -> extractFromZip(archive, executable, target)
            FfmpegArchiveFormat.TAR_XZ -> extractFromTarXz(archive, executable, target)
        }

        if (!extracted) {
            throw GradleException("No '$executable' entry found inside $archive")
        }
    }

    private fun extractFromZip(archive: File, executable: String, target: File): Boolean =
        ZipFile(archive).use { zip ->
            val entry = zip.entries()
                .asSequence()
                .firstOrNull { !it.isDirectory && it.name.isExecutablePath(executable) }
                ?: return@use false
            zip.getInputStream(entry).use { it.writeTo(target) }
            true
        }

    private fun extractFromTarXz(archive: File, executable: String, target: File): Boolean =
        TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(archive.inputStream()))).use { tar ->
            // `any` stops at the first match, leaving the stream positioned on that entry's data.
            val found = generateSequence { tar.nextEntry }
                .any { !it.isDirectory && it.name.isExecutablePath(executable) }
            if (!found) return@use false
            tar.writeTo(target)
            true
        }

    /**
     * Matches on the trailing path segment because the archives disagree about layout: the macOS
     * zips hold a bare `ffmpeg` at the root while the BtbN archives nest it under
     * `<build-name>/bin/`. `__MACOSX` holds resource fork stubs that share the file name.
     */
    private fun String.isExecutablePath(executable: String): Boolean =
        !startsWith("__MACOSX/") && substringAfterLast('/') == executable

    private fun InputStream.writeTo(target: File) {
        target.outputStream().buffered().use { output -> copyTo(output) }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val DOWNLOAD_ATTEMPTS = 3
        const val RETRY_BACKOFF_MILLIS = 2_000L
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val REQUEST_TIMEOUT_MINUTES = 10L

        /**
         * Some of the sources reject or challenge clients that do not present a browser-ish agent.
         */
        const val USER_AGENT = "Phovo-Gradle-Build"
    }
}
