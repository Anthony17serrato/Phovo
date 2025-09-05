package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.PhovoMediaEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.extensions.toMediaItem
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.extensions.toPhovoMediaEntity
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

open class CommonLocalSupportMediaRepository(
    private val localMediaDataSource: PhovoMediaDao,
    private val remoteMediaDataSource: MediaNetworkDataSource,
    private val localMediaProcessor: LocalMediaProcessor,
    logger: PhovoLogger,
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : LocalSupportMediaRepository(
    remotePhotosDataSource = remoteMediaDataSource,
    appScope = appScope
) {
    private val log = logger.withTag("LocalSupportMediaRepositoryImpl")

    // TODO: Implement paging
    final override fun phovoMediaFlow(): Flow<List<MediaItem>> {
        val remoteItemsFlow = super.phovoMediaFlow()
        val localItemsFlow = localMediaDataSource
            .observeAllDescendingTimestamp()
            .toMediaItems()

        return combine(remoteItemsFlow, localItemsFlow) { remote, local ->
            (local + remote).distinctBy { it.uri }
        }.flowOn(ioDispatcher)
    }

    final override fun initMediaProcessing(localDirectory: String?) {
        log.i { "initMediaProcessing" }
        // TODO Move to a periodic work manager
        appScope.launch {
            processJob(localDirectory)
            syncJob()
        }
    }

    protected open suspend fun handleProcessedMediaItem(mediaItem: MediaItem) {
        localMediaDataSource.insert(mediaItem.toPhovoMediaEntity())
    }

    private fun CoroutineScope.syncJob() = launch {
        // TODO
    }

    private fun CoroutineScope.processJob(localDirectory: String?) = launch {
        val localItems = localMediaDataSource
            .observeAllDescendingTimestamp()
            .toMediaItems()
            .first()
        val processMediaChannel = Channel<MediaItem>()
        with(localMediaProcessor) {
            processLocalItems(
                processedItems = localItems,
                localDirectory = localDirectory,
                processMediaChannel = processMediaChannel
            )
            consumeProcessedMedia(processMediaChannel)
        }
    }

    private fun CoroutineScope.consumeProcessedMedia(
        processMediaChannel: ReceiveChannel<MediaItem>
    ) = processMediaChannel.consumeAsFlow()
        .onEach(::handleProcessedMediaItem)
        .launchIn(this)

    private fun Flow<List<PhovoMediaEntity>>.toMediaItems(): Flow<List<MediaItem>> =
        map { localItems ->
            localItems.map {
                it.toMediaItem()
            }
        }
}