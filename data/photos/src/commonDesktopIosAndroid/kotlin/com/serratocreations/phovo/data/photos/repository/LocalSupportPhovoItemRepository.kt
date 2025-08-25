package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.local.LocalPhotoProvider
import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.model.PhovoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class LocalSupportPhovoItemRepository(
    private val localPhotosDataSource: LocalPhotoProvider,
    remotePhotosDataSource: PhotosNetworkDataSource,
    private val appScope: CoroutineScope
) : PhovoItemRepository(
    remotePhotosDataSource = remotePhotosDataSource,
    appScope = appScope
) {
    override fun phovoItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        // TODO: Implement local photos
        return super.phovoItemsFlow(localDirectory)
    }
}