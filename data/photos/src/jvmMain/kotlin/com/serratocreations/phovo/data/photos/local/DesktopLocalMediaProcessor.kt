package com.serratocreations.phovo.data.photos.local

import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.ExifTag
import com.ashampoo.kim.jvm.readMetadata
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import com.serratocreations.phovo.data.thumbnails.ThumbnailRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.metadata.XMPDM
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import java.io.FileInputStream
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

// TODO Investigate if both metadata parsers here can be replaced by FFMPEG
class DesktopLocalMediaProcessor(
    private val thumbnailRepository: ThumbnailRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val fileHashCalculator: FileHashCalculator,
    logger: PhovoLogger
) : LocalMediaProcessor {
    private val log = logger.withTag("DesktopLocalMediaProcessor")

    enum class FileType {
        Directory, Photo, Video, Other
    }
    private fun PlatformFile.getFileType(): FileType {
        if (this.isDirectory()) return FileType.Directory

        val extension = this.extension.lowercase()

        return when (extension) {
            in listOf("jpg", "jpeg", "png", "heic", "webp", "gif", "bmp", "tiff") -> FileType.Photo
            in listOf("mp4", "mov", "mkv", "avi", "webm", "flv") -> FileType.Video
            else -> FileType.Other
        }
    }

    override fun CoroutineScope.processLocalItems(
        processedItems: List<MediaItem>,
        // Todo update API signature to remove nullability
        localDirectory: String?,
        processMediaChannel: SendChannel<MediaItem>
    ) = launch(ioDispatcher) {

        val directory = localDirectory?.let { PlatformFile(localDirectory) } ?: run {
            log.e { "Directory is null" }
            return@launch
        }
        if (directory.exists().not()) {
            directory.createDirectories(mustCreate = false)
        }
        val directoryFiles = if (!directory.exists() || !directory.isDirectory()) {
            log.e { "Invalid directory: $localDirectory" }
            emptyList()
        } else {
            directory.list()
        }

        val processedItemIds = processedItems.map { it.uniqueAssetIdentifier }
        // TODO This work can be optimized with parallel decomposition
        directoryFiles.filter { availableFile ->
            // TODO check computation time, logic may need to revise for performance improvements
            val fileHash = fileHashCalculator.computeSha256(availableFile)
            fileHash !in processedItemIds
        }.forEach { file ->
            val fileType = file.getFileType()
            when (fileType) {
                FileType.Directory -> {
                    // TODO: Some recursion implementation
                    null
                }

                FileType.Photo -> {
                    processImage(file, localDirectory)
                }

                FileType.Video -> {
                    processVideo(file, localDirectory)
                }

                FileType.Other -> null
            }?.let { mediaItem ->
                processMediaChannel.send(mediaItem)
            }
        }
    }

    // TODO Migrate to Ffmpeg for metadata extraction
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    private suspend fun processVideo(
        file: PlatformFile,
        outputDirectory: String
    ): MediaVideoItem? = withContext(ioDispatcher) {
        val metadata = Metadata()
        FileInputStream(file.file).use { stream ->
            val parser = AutoDetectParser()
            val handler = BodyContentHandler()
            parser.parse(stream, handler, metadata)
        }

        val durationSeconds = metadata.get(XMPDM.DURATION)?.toDoubleOrNull()?.toLong() ?: 0L
        val creationDate: LocalDateTime =
            metadata.get(TikaCoreProperties.CREATED)?.let { creationDate ->
                runCatching {
                    Instant.parse(creationDate)
                }.getOrNull()?.toLocalDateTime(TimeZone.UTC)
            } ?: return@withContext null // TODO find other methods to get a date

        val sha256Hash = fileHashCalculator.computeSha256(file)
        thumbnailRepository.generateVideoThumbnails(
            rootOutputDirectory = PlatformFile(outputDirectory),
            videoFile = file,
            thumbnailName = sha256Hash
        )
        return@withContext MediaVideoItem(
            assetLocation = LocalOrRemoteAsset.LocalAsset(
                localAssetLocation = file,
                isAlsoAvailableRemotely = true
            ),
            fileName = file.name,
            dateInFeed = creationDate,
            size = file.size(),
            duration = durationSeconds.seconds,
            uniqueAssetIdentifier = sha256Hash
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun processImage(
        file: PlatformFile,
        outputDirectory: String
    ): MediaImageItem? = withContext(ioDispatcher) {
        val metadata = Kim.readMetadata(file.file)
        val takenDate = metadata?.findStringValue(ExifTag.EXIF_TAG_DATE_TIME_ORIGINAL)
            ?: return@withContext null
        // Define the custom format pattern
        val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

        val sha256Hash = fileHashCalculator.computeSha256(file)
        thumbnailRepository.generateImageThumbnails(
            rootOutputDirectory = PlatformFile(outputDirectory),
            imageFile = file,
            thumbnailName = sha256Hash
        )
        return@withContext MediaImageItem(
            assetLocation = LocalOrRemoteAsset.LocalAsset(
                localAssetLocation = file,
                isAlsoAvailableRemotely = true
            ),
            fileName = file.name,
            dateInFeed = takenDate.let { date ->
                java.time.LocalDateTime.parse(date, formatter).toKotlinLocalDateTime()
            },
            // TODO
            size = file.size(),
            uniqueAssetIdentifier = sha256Hash
        )
    }
}