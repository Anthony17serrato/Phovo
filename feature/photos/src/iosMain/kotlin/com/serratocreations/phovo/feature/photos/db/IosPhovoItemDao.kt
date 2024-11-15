package com.serratocreations.phovo.feature.photos.db

import com.serratocreations.phovo.feature.photos.data.db.dao.PhovoItemDao
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import kotlinx.coroutines.flow.Flow
import platform.Foundation.NSURL
import coil3.toUri
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoImageItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
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
            emit(fetchGalleryImageURLs()
                .mapNotNull { it.absoluteString()?.toUri() }
                .map { PhovoImageItem(it, "", 0) })
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
    private suspend fun fetchGalleryImageURLs(): List<NSURL> = coroutineScope {
        val status = PHPhotoLibrary.authorizationStatus()
        println("Photo Library Authorization Status: $status")
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val urls = mutableListOf<NSURL>()

        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            val resourceOptions = PHAssetResourceRequestOptions()
            resourceOptions.networkAccessAllowed = true

            val resources = PHAssetResource.assetResourcesForAsset(asset)

            // Safely cast the first resource to PHAssetResource
            val resource = resources.firstOrNull() as? PHAssetResource ?: return@enumerateObjectsUsingBlock
            resource.assetLocalIdentifier
            println("Resource $resource")
            val tempDir = NSTemporaryDirectory()
            val tempFileURL = NSURL.fileURLWithPath("$tempDir/${resource.originalFilename}")
            launch {
                // Delete existing file if it exists
                val fileManager = NSFileManager.defaultManager
                tempFileURL.path?.let { pathNotNull ->
                    if (fileManager.fileExistsAtPath(pathNotNull)) {
                        fileManager.removeItemAtURL(tempFileURL, null)
                    }
                }
                val result = suspendCoroutine { continuation ->
                    PHAssetResourceManager.defaultManager().writeDataForAssetResource(
                        resource,
                        tempFileURL,
                        options = resourceOptions,
                        completionHandler = { error ->
                            if (error == null) {
                                println("Success $tempFileURL")
                                continuation.resume(tempFileURL)
                            } else {
                                println("PHAssetResourceManager completion error $error")
                                continuation.resume(null)
                            }
                        }
                    )
                }
                result?.let { urls.add(it) }
            }
        }
        return@coroutineScope urls
    }
}