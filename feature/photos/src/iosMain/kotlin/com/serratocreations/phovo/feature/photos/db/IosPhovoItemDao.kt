package com.serratocreations.phovo.feature.photos.db

import com.serratocreations.phovo.feature.photos.data.db.dao.PhovoItemDao
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import coil3.toUri
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoImageItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import platform.Photos.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosPhovoItemDao() : PhovoItemDao {
    override fun addItem(phovoItem: PhovoItem) {
        TODO("Not yet implemented")
    }

    override fun allItemsFlow(): Flow<List<PhovoItem>> {
        return flow {
            requestPhotoLibraryPermission()
            emit(fetchGalleryImageURLs().map { PhovoImageItem(it.toUri(), "",null, 0) })
        }
    }

    override fun updatePhovoItem(phovoItem: PhovoItem): Boolean {
        TODO("Not yet implemented")
    }

    private suspend fun requestPhotoLibraryPermission() = suspendCoroutine { continuation ->
        PHPhotoLibrary.requestAuthorization { status ->
            when (status) {
                PHAuthorizationStatusAuthorized -> {
                    println("Photo Library access granted")
                    // Proceed with photo fetching logic
                }
                PHAuthorizationStatusDenied -> {
                    println("Photo Library access denied")
                    // Guide the user to settings if needed
                }
                PHAuthorizationStatusRestricted -> {
                    println("Photo Library access restricted")
                    // Handle the case for restricted access (e.g., parental controls)
                }
                PHAuthorizationStatusNotDetermined -> {
                    println("Photo Library access not determined")
                    // The user hasn't been asked yet, possibly retry request
                }
            }
            continuation.resume(Unit)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun fetchGalleryImageURLs(): List<String> = coroutineScope {
        val status = PHPhotoLibrary.authorizationStatus()
        println("Photo Library Authorization Status: $status")
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val urls = mutableListOf<String>()

        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            urls.add("phasset://${asset.localIdentifier}")
        }
        println("IosPhovoItemDao urls $urls")
        return@coroutineScope urls
    }
}