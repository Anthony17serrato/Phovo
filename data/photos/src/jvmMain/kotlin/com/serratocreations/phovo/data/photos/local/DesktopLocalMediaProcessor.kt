package com.serratocreations.phovo.data.photos.local

import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.ExifTag
import com.ashampoo.kim.jvm.readMetadata
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import com.serratocreations.phovo.data.thumbnails.ThumbnailRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.CoroutineDispatcher
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
) {
    private val log = logger.withTag("DesktopLocalMediaProcessor")

    enum class FileType {
        Directory, Photo, Video, Other
    }

    // TODO Migrate to Ffmpeg for metadata extraction
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    suspend fun processVideo(
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
            assetLocation = AssetLocation.LocalAssetLocation(
                localAssetLocation = file
            ),
            isSynced = true,
            fileName = file.name,
            dateInFeed = creationDate,
            size = file.size(),
            duration = durationSeconds.seconds,
            uniqueAssetIdentifier = sha256Hash
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun processImage(
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
            assetLocation = AssetLocation.LocalAssetLocation(
                localAssetLocation = file
            ),
            isSynced = true,
            fileName = file.name,
            dateInFeed = takenDate.let { date ->
                java.time.LocalDateTime.parse(date, formatter).toKotlinLocalDateTime()
            },
            size = file.size(),
            uniqueAssetIdentifier = sha256Hash
        )
    }
}