package com.serratocreations.phovo.data.photos.local

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaDimensions
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation
import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import com.serratocreations.phovo.data.thumbnails.FfprobeMediaProbe
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
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class DesktopLocalMediaProcessor(
    private val thumbnailRepository: ThumbnailRepository,
    private val mediaProbe: FfprobeMediaProbe,
    private val ioDispatcher: CoroutineDispatcher,
    private val fileHashCalculator: FileHashCalculator,
    logger: PhovoLogger
) {
    private val log = logger.withTag("DesktopLocalMediaProcessor")

    enum class FileType {
        Directory, Photo, Video, Other, Partial
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    suspend fun processVideo(
        file: PlatformFile,
        outputDirectory: String
    ): MediaVideoItem? = withContext(ioDispatcher) {
        val probe = mediaProbe.probeVideo(file) ?: return@withContext null
        val dimensions = probe.dimensions ?: run {
            log.e { "Skipping ${file.name}, could not read its dimensions" }
            return@withContext null
        }
        val creationDate: LocalDateTime = probe.creationTime?.toLocalDateTime(TimeZone.UTC)
            ?: return@withContext null // TODO find other methods to get a date

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
            duration = probe.duration ?: Duration.ZERO,
            uniqueAssetIdentifier = sha256Hash,
            dimensions = dimensions
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun processImage(
        file: PlatformFile,
        outputDirectory: String
    ): MediaImageItem? = withContext(ioDispatcher) {
        val metadata = try {
            ImageMetadataReader.readMetadata(file.file)
        } catch (e: ImageProcessingException) {
            log.e { "Skipping ${file.name}, could not read its metadata: $e" }
            return@withContext null
        } catch (e: IOException) {
            log.e { "Skipping ${file.name}, could not read its metadata: $e" }
            return@withContext null
        }
        val takenDate = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
            ?.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
            ?.let(::parseExifDateTime)
            ?: return@withContext null
        // The size comes from ffprobe rather than metadata-extractor, see probeImageSize.
        // EXIF defines a missing orientation as 1, normal.
        val orientation = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            ?.getInteger(ExifIFD0Directory.TAG_ORIENTATION)
            ?: 1
        if (orientation !in MediaDimensions.EXIF_ORIENTATIONS) {
            log.e { "Skipping ${file.name}, EXIF orientation $orientation is out of range" }
            return@withContext null
        }
        val dimensions = mediaProbe.probeImageSize(file)?.withExifOrientation(orientation) ?: run {
            log.e { "Skipping ${file.name}, could not read its dimensions" }
            return@withContext null
        }

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
            dateInFeed = takenDate,
            size = file.size(),
            uniqueAssetIdentifier = sha256Hash,
            dimensions = dimensions
        )
    }
}

/**
 * Parses the Exif date format, `yyyy:MM:dd HH:mm:ss`. Cameras that do not know the date fill it
 * with spaces and colons, which comes back as null.
 */
private fun parseExifDateTime(value: String): LocalDateTime? = try {
    java.time.LocalDateTime.parse(value.trim(), EXIF_DATE_TIME_FORMAT).toKotlinLocalDateTime()
} catch (e: DateTimeParseException) {
    null
}

private val EXIF_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
