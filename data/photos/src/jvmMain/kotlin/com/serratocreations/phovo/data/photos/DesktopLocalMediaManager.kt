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
        outputDirectory: String
    ) {
        log.i { "initMediaProcessing" }
        appScope.launch {
            val processJob = processJob(
                outputDirectory = outputDirectory
            )
        }
    }

    private suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localMediaRepository.addOrUpdateMediaItem(mediaItem)
    }

    private fun CoroutineScope.processJob(
        outputDirectory: String
    ) = launch {
        val processMediaChannel = Channel<MediaItem>()
        appScope.consumeProcessedMedia(processMediaChannel)

        launch {
            processKnownLocalItems(
                outputDirectory = outputDirectory,
                processMediaChannel = processMediaChannel
            )
        }
        launch(ioDispatcher) {
            // TODO Need to implement a strategy to rescan unknown items periodically
            //  (As configured by the user)
            processUnknownLocalItems(
                outputDirectory = outputDirectory,
                processMediaChannel = processMediaChannel
            )
        }
    }

    /**
     * Process media items which are known to the Desktop server but
     * are missing metadata and thumbnail extractions.
     */
    suspend fun processKnownLocalItems(
        outputDirectory: String,
        processMediaChannel: SendChannel<MediaItem>
    ) {
        // TODO This will get stuck if a media item fails to process, need to
        //  implement a way to filter items which failed to process.
        localMediaRepository.observeFirstUnprocessedFullLocalMedia().collect { localItemToProcess ->
            if (localItemToProcess == null) {
                log.i { "All known items have been processed currently" }
                return@collect
            }
            val fileToProcess = PlatformFile(localItemToProcess.localUri)
            fileToProcess.mapAndProcessFile(outputDirectory, processMediaChannel)
        }
    }

    /**
     * Some local items may exist in the users backup directory but
     * are not known to the Phovo applications, these items need to be processed
     * in order to become known to Phovo.
     */
    suspend fun processUnknownLocalItems(
        outputDirectory: String,
        processMediaChannel: SendChannel<MediaItem>
    ) {
        val directory = PlatformFile(outputDirectory)
        if (directory.exists().not()) {
            directory.createDirectories(mustCreate = false)
        }
        val directoryFiles = if (!directory.exists() || !directory.isDirectory()) {
            log.e { "Invalid directory: $outputDirectory" }
            emptyList()
        } else {
            directory.list()
        }

        // TODO This work can be optimized with parallel decomposition
        val unProcessedUnknownFilesFlow: Flow<PlatformFile> = flow {
            directoryFiles.forEach { directoryChild ->
                if (directoryChild.isDirectory()) return@forEach
                val fileHash = fileHashCalculator.computeSha256(directoryChild)
                val existingKnownAsset = localMediaRepository.getLocalMediaByAssetHash(fileHash)
                // asset is not known, process it
                if (existingKnownAsset == null) emit(directoryChild)
            }
        }
        // TODO This work can be optimized with parallel decomposition
        unProcessedUnknownFilesFlow.collect { file ->
            file.mapAndProcessFile(outputDirectory, processMediaChannel)
        }
    }

    private suspend fun PlatformFile.mapAndProcessFile(
        outputDirectory: String,
        processMediaChannel: SendChannel<MediaItem>
    ) {
        val fileType = this.getFileType()
        when (fileType) {
            FileType.Directory -> {
                // TODO: Some recursion implementation
                null
            }

            FileType.Photo -> {
                localMediaProcessor.processImage(this, outputDirectory)
            }

            FileType.Video -> {
                localMediaProcessor.processVideo(this, outputDirectory)
            }

            FileType.Other -> null
        }?.let { mediaItem ->
            processMediaChannel.send(mediaItem)
        }
    }

    private fun PlatformFile.getFileType(): FileType {
        if (this.isDirectory()) return FileType.Directory

        val extension = this.extension.lowercase()
        // TODO this logic can be problematic if file is not actually what the extension says
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