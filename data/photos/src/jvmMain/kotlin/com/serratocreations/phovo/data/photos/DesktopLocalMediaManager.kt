package com.serratocreations.phovo.data.photos

import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor.FileType
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class DesktopLocalMediaManager(
    private val localMediaRepository: LocalMediaRepository,
    private val localMediaProcessor: DesktopLocalMediaProcessor,
    private val ioDispatcher: CoroutineDispatcher,
    private val fileHashCalculator: FileHashCalculator,
    private val appScope: CoroutineScope,
    logger: PhovoLogger,
) {
    private val log = logger.withTag("LocalMediaManager")
    companion object {
        // We need at least one worker
        private val WORKER_COUNT = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
    }

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

        appScope.processJob(
            outputDirectory = outputDirectory
        )
    }

    private fun CoroutineScope.processJob(
        outputDirectory: String
    ) = launch {
        val processMediaChannel = Channel<PlatformFile>()

        repeat(WORKER_COUNT) {
            processMediaWorker(processMediaChannel, outputDirectory)
        }

        launch {
            processKnownLocalItems(
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
        processMediaChannel: SendChannel<PlatformFile>
    ) {
        localMediaRepository.observeFirstUnprocessedFullLocalMedia().collect { localItemToProcess ->
            if (localItemToProcess == null) {
                log.i { "All known items have been processed currently" }
                return@collect
            }
            val claimed = localMediaRepository.tryProcessingClaim(localItemToProcess.assetHash)
            if (claimed) {
                processMediaChannel.send(PlatformFile(localItemToProcess.localUri))
            }
        }
    }

    /**
     * Some local items may exist in the users backup directory but
     * are not known to the Phovo applications, these items need to be processed
     * in order to become known to Phovo.
     */
    suspend fun processUnknownLocalItems(
        outputDirectory: String,
        processMediaChannel: SendChannel<PlatformFile>
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

        val unProcessedUnknownFilesFlow: Flow<PlatformFile> = flow {
            // TODO files may become very large, investigate memory optimizations
            directoryFiles.forEach { directoryChild ->
                val fileType = directoryChild.getFileType()
                if (fileType in setOf(FileType.Other, FileType.Directory, FileType.Partial)) {
                    log.w { "Skipping unsupported directory child $fileType extension ${directoryChild.extension}" }
                    return@forEach
                }
                yield()
                // TODO This work can be optimized with parallel decomposition
                val fileHash = fileHashCalculator.computeSha256(directoryChild)
                val existingKnownAsset = localMediaRepository.getLocalMediaByAssetHash(fileHash)
                // asset is not known, process it
                if (existingKnownAsset == null) emit(directoryChild)
            }
        }

        unProcessedUnknownFilesFlow.collect { file ->
            processMediaChannel.send(file)
        }
    }

    private suspend fun PlatformFile.mapAndProcessFile(
        outputDirectory: String
    ) {
        val fileType = this.getFileType()
        when (fileType) {
            FileType.Directory -> {
                // TODO: Some recursion implementation
                log.w { "file $this is a directory" }
                null
            }

            FileType.Photo -> {
                localMediaProcessor.processImage(this, outputDirectory)
            }

            FileType.Video -> {
                localMediaProcessor.processVideo(this, outputDirectory)
            }

            FileType.Other, FileType.Partial -> null
        }?.let { mediaItem ->
            localMediaRepository.addOrUpdateMediaItem(mediaItem)
            localMediaRepository.removeProcessingClaim(mediaItem.uniqueAssetIdentifier)
        }
    }

    // TODO check if PlatformFile offers better APIs
    private fun PlatformFile.getFileType(): FileType {
        if (this.isDirectory()) return FileType.Directory

        val extension = this.extension.lowercase()
        // TODO this logic can be problematic if file is not actually what the extension says
        return when (extension) {
            "part" -> FileType.Partial
            in listOf("jpg", "jpeg", "png", "heic", "webp", "gif", "bmp", "tiff") -> FileType.Photo
            in listOf("mp4", "mov", "mkv", "avi", "webm", "flv") -> FileType.Video
            else -> FileType.Other
        }
    }

    private fun CoroutineScope.processMediaWorker(
        processMediaChannel: ReceiveChannel<PlatformFile>,
        outputDirectory: String
    ) = launch {
        for (fileToProcess in processMediaChannel) {
            yield()
            fileToProcess.mapAndProcessFile(outputDirectory)
        }
    }
}