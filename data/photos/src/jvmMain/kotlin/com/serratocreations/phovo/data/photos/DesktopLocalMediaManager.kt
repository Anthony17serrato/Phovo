package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor.FileType
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DesktopLocalMediaManager(
    private val localMediaRepository: LocalMediaRepository,
    private val localMediaProcessor: DesktopLocalMediaProcessor,
    private val ioDispatcher: CoroutineDispatcher,
    private val fileHashCalculator: FileHashCalculator,
    private val appScope: CoroutineScope,
    logger: PhovoLogger,
) {
    private val log = logger.withTag("LocalMediaManager")

    /**
     * API initializes job to process local media and synchronize to server.
     * Processing includes tasks such as extracting media metadata and generating md5 hashes and
     * deduplication logic
     */

    fun initMediaProcessing(
        // TODO Use PlatformFile
        localDirectory: String
    ) {
        log.i { "initMediaProcessing" }
        appScope.launch {
            val processJob = processJob(
                localDirectory = localDirectory
            )
        }
    }

    private suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localMediaRepository.addOrUpdateMediaItem(mediaItem)
    }

    private fun CoroutineScope.processJob(
        localDirectory: String
    ) = launch {
        val processMediaChannel = Channel<MediaItem>()
        appScope.consumeProcessedMedia(processMediaChannel)

        processUnknownLocalItems(
            localDirectory = localDirectory,
            processMediaChannel = processMediaChannel
        ).join()
        processMediaChannel.close()
    }

    /**
     * Some local items may exist in the users backup directory but
     * are not known to the Phovo applications, these items need to be processed
     * in order to become known to Phovo.
     */
    fun CoroutineScope.processUnknownLocalItems(
        localDirectory: String,
        processMediaChannel: SendChannel<MediaItem>
    ) = launch(ioDispatcher) {
        val directory = PlatformFile(localDirectory)
        if (directory.exists().not()) {
            directory.createDirectories(mustCreate = false)
        }
        val directoryFiles = if (!directory.exists() || !directory.isDirectory()) {
            log.e { "Invalid directory: $localDirectory" }
            emptyList()
        } else {
            directory.list()
        }

        // TODO This work can be optimized with parallel decomposition
        val unProcessedFilesFlow: Flow<PlatformFile> = flow {
            directoryFiles.forEach { directoryChild ->
                if (directoryChild.isDirectory()) return@forEach
                val fileHash = fileHashCalculator.computeSha256(directoryChild)
                val existingKnownAsset = localMediaRepository.getLocalMediaByAssetHash(fileHash)
                // asset is not known, process it
                if (existingKnownAsset == null) emit(directoryChild)
            }
        }
        // TODO This work can be optimized with parallel decomposition
        unProcessedFilesFlow.collect { file ->
            val fileType = file.getFileType()
            when (fileType) {
                FileType.Directory -> {
                    // TODO: Some recursion implementation
                    null
                }

                FileType.Photo -> {
                    localMediaProcessor.processImage(file, localDirectory)
                }

                FileType.Video -> {
                    localMediaProcessor.processVideo(file, localDirectory)
                }

                FileType.Other -> null
            }?.let { mediaItem ->
                processMediaChannel.send(mediaItem)
            }
        }
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

    private fun CoroutineScope.consumeProcessedMedia(
        processMediaChannel: ReceiveChannel<MediaItem>
    ) = processMediaChannel.consumeAsFlow()
        .onEach(::handleProcessedMediaItem)
        .launchIn(this)
}