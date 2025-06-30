package com.serratocreations.phovo.data.photos.db.dao

import coil3.Uri
import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.ExifTag
import com.ashampoo.kim.jvm.readMetadata
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDateTime
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.format.DateTimeFormatter

class DesktopPhovoItemDao(
    logger: PhovoLogger,
    private val ioDispatcher: CoroutineDispatcher
) : PhovoItemDao {
    private val log = logger.withTag("DesktopPhovoItemDao")

    // TODO temporary implementation, this API should observe the table of synced images from database
    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        return channelFlow<List<PhovoItem>> {
            val dirPath = localDirectory?.let { Paths.get(it) }
            if (dirPath != null && !Files.exists(dirPath)) {
                Files.createDirectories(dirPath)
            }
            val processedImages = mutableListOf<PhovoItem>()
            val filesChannel = Channel<List<File>>(Channel.UNLIMITED)
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
                val newlyProcessedImages = it.filter { unprocessedImage ->
                    unprocessedImage.name !in processedImages.map { image -> image.name }
                }.mapNotNull { image ->
                    val metadata = Kim.readMetadata(image)
                    val takenDate = metadata?.findStringValue(ExifTag.EXIF_TAG_DATE_TIME_ORIGINAL) ?: return@mapNotNull null
                    // Define the custom format pattern
                    val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                    PhovoImageItem(
                        uri = Uri(scheme = "file", path = image.toURI().path),
                        name = image.name,
                        dateInFeed = takenDate.let {
                            date -> JavaLocalDateTime.parse(date, formatter).toKotlinLocalDateTime()
                        },
                        size = 0
                    )
                }
                processedImages.addAll(newlyProcessedImages)
                send(processedImages)
            }
        }.flowOn(ioDispatcher)
    }
}