package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.local.LocalUnprocessedMediaProvider
import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.model.PhovoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class LocalSupportMediaRepositoryImpl(
    private val localUnprocessedMediaProvider: LocalUnprocessedMediaProvider,
    private val localProcessedMediaDataSource: PhovoMediaDao,
    remotePhotosDataSource: PhotosNetworkDataSource,
    logger: PhovoLogger,
    private val appScope: CoroutineScope
) : LocalSupportMediaRepository(
    remotePhotosDataSource = remotePhotosDataSource,
    appScope = appScope
) {
    private val log = logger.withTag("LocalSupportMediaRepositoryImpl")

    override fun phovoItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        val remoteItemsFlow = super.phovoItemsFlow(localDirectory)
        // TODO: Implement local photos
        return super.phovoItemsFlow(localDirectory)
    }

    override fun initMediaProcessing() {
        log.i { "initMediaProcessing" }
    }
}