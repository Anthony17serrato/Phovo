package com.serratocreations.phovo.data.thumbnails

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaDimensions
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * What ffprobe could read from a video. Each field is null when the file does not record it, and
 * [dimensions] is also null when the recorded size or rotation is out of range.
 */
data class VideoProbe @OptIn(ExperimentalTime::class) constructor(
    val dimensions: MediaDimensions?,
    val duration: Duration?,
    val creationTime: Instant?
)

/** Reads media metadata with the bundled ffprobe. */
class FfprobeMediaProbe(
    private val tools: BundledFfmpegTools,
    private val ioDispatcher: CoroutineDispatcher,
    logger: PhovoLogger
) {
    private val log = logger.withTag("FfprobeMediaProbe")

    /** Returns null when ffprobe could not read [videoFile] at all. */
    suspend fun probeVideo(videoFile: PlatformFile): VideoProbe? =
        runFfprobe(
            file = videoFile,
            "-select_streams", "v:0",
            "-show_entries",
            "format=duration:format_tags=creation_time:" +
                "stream=width,height:stream_tags=rotate:stream_side_data=rotation"
        )?.let(::parseFfprobeVideoOutput)

    /**
     * Returns the size [imageFile] is stored at, before its EXIF orientation is applied, or null
     * when ffprobe cannot read it. Used for every
     * photo format, because metadata-extractor reports the size of a single tile for tiled HEIC
     * images, which is every photo an iPhone takes:
     * https://github.com/drewnoakes/metadata-extractor/issues/373
     */
    suspend fun probeImageSize(imageFile: PlatformFile): MediaDimensions? =
        runFfprobe(
            file = imageFile,
            "-show_entries",
            "stream=width,height:stream_disposition=default:" +
                "stream_group=type:stream_group_component=width,height"
        )?.let(::parseFfprobeImageSizeOutput)

    /** Runs ffprobe with [arguments] and JSON output, returning what it printed. */
    private suspend fun runFfprobe(file: PlatformFile, vararg arguments: String): String? =
        withContext(ioDispatcher) {
            val command = listOf(tools.ffprobe.await().absolutePath(), "-v", "error") +
                arguments + listOf("-of", "json", file.absolutePath())
            try {
                val process = ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                // Read before waiting, so a full output pipe cannot stall the process.
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (!process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    log.e { "ffprobe timed out on ${file.absolutePath()}" }
                    return@withContext null
                }
                if (process.exitValue() != 0) {
                    log.e { "ffprobe exited with ${process.exitValue()} on ${file.absolutePath()}" }
                    return@withContext null
                }
                output
            } catch (e: IOException) {
                log.e { "ffprobe failed on ${file.absolutePath()}: $e" }
                null
            }
        }

    private companion object {
        const val PROBE_TIMEOUT_SECONDS = 30L
    }
}

/**
 * Parses the JSON printed by [FfprobeMediaProbe.probeVideo]. Rotation comes from the display matrix
 * side data, or from the `rotate` tag that older muxers wrote instead.
 */
@OptIn(ExperimentalTime::class)
internal fun parseFfprobeVideoOutput(output: String): VideoProbe? {
    val root = parseJsonObject(output) ?: return null
    val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
    val format = root["format"]?.jsonObject

    // Videos without a rotation are not rotated.
    val rotation = stream?.get("side_data_list")?.jsonArray
        ?.firstNotNullOfOrNull { (it as? JsonObject)?.get("rotation")?.jsonPrimitive?.intOrNull }
        ?: stream?.tag("rotate")?.toIntOrNull()
        ?: 0
    val width = stream?.get("width")?.jsonPrimitive?.intOrNull
    val height = stream?.get("height")?.jsonPrimitive?.intOrNull
    val dimensions = if (width != null && height != null && width > 0 && height > 0 && rotation % 90 == 0) {
        MediaDimensions(width, height).rotatedBy(rotation)
    } else {
        null
    }
    return VideoProbe(
        dimensions = dimensions,
        duration = format?.get("duration")?.jsonPrimitive?.contentOrNull
            ?.toDoubleOrNull()?.seconds,
        creationTime = format?.tag("creation_time")
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
    )
}

private fun JsonObject.tag(name: String): String? =
    (get("tags") as? JsonObject)?.get(name)?.jsonPrimitive?.contentOrNull

/**
 * Parses the JSON printed by [FfprobeMediaProbe.probeImageSize]. HEIF images may hold several
 * images, so the size is picked in this order:
 *
 * 1. A tile grid, which is how large HEIC and AVIF images are stored. Its streams are the tiles.
 * 2. The stream marked default, which is the primary image when the file also holds a thumbnail.
 * 3. The largest stream, for formats that mark nothing as default, such as JPEG.
 */
internal fun parseFfprobeImageSizeOutput(output: String): MediaDimensions? {
    val root = parseJsonObject(output) ?: return null

    val gridSize = root["stream_groups"]?.jsonArray
        ?.mapNotNull { it as? JsonObject }
        ?.firstOrNull { it.string("type") == "Tile Grid" }
        ?.get("components")?.jsonArray
        ?.firstOrNull()?.let { it as? JsonObject }
        ?.size()
    if (gridSize != null) return gridSize

    val streams = root["streams"]?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()
    val defaultStream = streams.firstOrNull {
        (it["disposition"] as? JsonObject)?.get("default")?.jsonPrimitive?.intOrNull == 1
    }
    return defaultStream?.size()
        ?: streams.mapNotNull { it.size() }.maxByOrNull { it.width.toLong() * it.height }
}

private fun parseJsonObject(output: String): JsonObject? = try {
    Json.parseToJsonElement(output) as? JsonObject
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

private fun JsonObject.string(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

/** The stream's size, or null when it is missing or not positive. */
private fun JsonObject.size(): MediaDimensions? {
    val width = (get("width") as? JsonPrimitive)?.intOrNull ?: return null
    val height = (get("height") as? JsonPrimitive)?.intOrNull ?: return null
    return if (width > 0 && height > 0) MediaDimensions(width, height) else null
}
