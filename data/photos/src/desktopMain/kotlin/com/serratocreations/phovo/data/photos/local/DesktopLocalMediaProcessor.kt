package com.serratocreations.phovo.data.photos.local

import coil3.Uri
import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.ExifTag
import com.ashampoo.kim.jvm.readMetadata
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.metadata.XMPDM
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

// TODO Investigate if both metadata parsers here can be replaced by FFMPEG
class DesktopLocalMediaProcessor(
    logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher
) : LocalMediaProcessor {
    private val log = logger.withTag("DesktopPhovoItemDao")

    enum class FileType {
        Directory, Photo, Video, Other
    }
    private fun File.getFileType(): FileType {
        if (isDirectory) return FileType.Directory

        val extension = extension.lowercase()

        return when (extension) {
            in listOf("jpg", "jpeg", "png", "heic", "webp", "gif", "bmp", "tiff") -> FileType.Photo
            in listOf("mp4", "mov", "mkv", "avi", "webm", "flv") -> FileType.Video
            else -> FileType.Other
        }
    }

    override fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        localDirectory: String?,
        processMediaChannel: SendChannel<MediaItem>
    ): Job {
        TODO("Not yet implemented")
    }

    // TODO temporary implementation, this API should observe the table of synced images from database
    fun processLocalItems(localDirectory: String?): Flow<List<MediaItem>> {
        return channelFlow<List<MediaItem>> {
            val dirPath = localDirectory?.let { Paths.get(it) }
            if (dirPath != null && !Files.exists(dirPath)) {
                Files.createDirectories(dirPath)
            }
            val processedMediaItems = mutableListOf<MediaItem>()
            val filesChannel = Channel<List<File>>(Channel.Factory.UNLIMITED)
            launch {
                while (true) {
                    val directory = localDirectory?.let { File(it) }
                    if (directory == null || !directory.exists() || !directory.isDirectory) {
                        log.e { "Invalid directory: $localDirectory" }
                        filesChannel.send(emptyList())
                    } else {
                        filesChannel.send(directory.listFiles()?.toList() ?: emptyList())
                    }
                    delay(1_000L)
                }
            }
            filesChannel.consumeAsFlow().collect {
                val newlyProcessedPhovoItems = it.filter { unprocessedItems ->
                    unprocessedItems.name !in processedMediaItems.map { item -> item.name }
                }.mapNotNull { file ->
                    val fileType = file.getFileType()
                    return@mapNotNull when (fileType) {
                        FileType.Directory -> {
                            // TODO: Some recursion implementation
                            null
                        }

                        FileType.Photo -> {
                            processImage(file)
                        }

                        FileType.Video -> {
                            processVideo(file)
                        }

                        FileType.Other -> null
                    }
                }
                processedMediaItems.addAll(newlyProcessedPhovoItems)
                send(processedMediaItems)
            }
        }.flowOn(ioDispatcher)
    }

    private suspend fun processVideo(file: File): MediaVideoItem? = withContext(ioDispatcher) {
        val metadata = Metadata()
        FileInputStream(file).use { stream ->
            val parser = AutoDetectParser()
            val handler = BodyContentHandler()
            parser.parse(stream, handler, metadata)
        }

        val durationSeconds = metadata.get(XMPDM.DURATION)?.toDoubleOrNull()?.toLong() ?: 0L
        val creationDate: LocalDateTime =
            metadata.get(TikaCoreProperties.CREATED)?.let { creationDate ->
                runCatching {
                    Instant.Companion.parse(creationDate)
                }.getOrNull()?.toLocalDateTime(TimeZone.Companion.UTC)
            } ?: return@withContext null // TODO find other methods to get a date

        return@withContext MediaVideoItem(
            uri = Uri(scheme = "file", path = file.toURI().path),
            name = file.name,
            dateInFeed = creationDate,
            size = file.length().toInt(),
            duration = durationSeconds.seconds
        )
    }

    private suspend fun processImage(file: File): MediaImageItem? = withContext(ioDispatcher) {
        val metadata = Kim.readMetadata(file)
        val takenDate = metadata?.findStringValue(ExifTag.EXIF_TAG_DATE_TIME_ORIGINAL)
            ?: return@withContext null
        // Define the custom format pattern
        val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
        return@withContext MediaImageItem(
            uri = Uri(scheme = "file", path = file.toURI().path),
            name = file.name,
            dateInFeed = takenDate.let { date ->
                java.time.LocalDateTime.parse(date, formatter).toKotlinLocalDateTime()
            },
            size = 0
        )
    }
}