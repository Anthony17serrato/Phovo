package com.serratocreations.phovo.data.photos.db

import kotlinx.coroutines.flow.Flow
import coil3.toUri
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.db.entity.PhovoItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Singleton
import platform.Photos.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class IosPhovoItemDao : PhovoItemDao {

    override fun allItemsFlow(localDirectory: String?): Flow<List<PhovoItem>> {
        return flow {
            requestPhotoLibraryPermission()
            emit(
                fetchGalleryImageURLs()
            )
        }
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
    private suspend fun fetchGalleryImageURLs(): List<PhovoImageItem> = coroutineScope {
        val status = PHPhotoLibrary.authorizationStatus()
        println("Photo Library Authorization Status: $status")
        val fetchOptions = PHFetchOptions()
        val assets = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, fetchOptions)
        val imageItems = mutableListOf<PhovoImageItem>()

        // Enumerate the assets using the block-based approach
        assets.enumerateObjectsUsingBlock { obj, _, _ ->
            val asset = obj as PHAsset
            val instant = asset.creationDate?.toKotlinInstant()
            // TODO: Instead of excluding images where date could not be determined parse the date from exif data
            val localDateTime = instant?.toLocalDateTime(TimeZone.currentSystemDefault()) ?: return@enumerateObjectsUsingBlock
            val phovoImageItem = PhovoImageItem(
                uri = "phasset://${asset.localIdentifier}".toUri(),
                name = "",
                dateInFeed = localDateTime,
                size = 0
            )
            imageItems.add(phovoImageItem)
        }
        println("IosPhovoItemDao images $imageItems")
        return@coroutineScope imageItems.toList()
    }
}