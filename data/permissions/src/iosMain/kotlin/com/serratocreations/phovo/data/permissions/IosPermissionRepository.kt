package com.serratocreations.phovo.data.permissions

import com.serratocreations.phovo.data.permissions.annotations.DelicatePermissionsApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHChange
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHPhotoLibraryChangeObserverProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

class IosPermissionRepository : PermissionRepository {

    private val _permissionEvents = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // TODO This is not the correct place to have this logic but this code detects when photoLibrary
    //  changes such as photo picker updates or new library photos available, it should be updated
    //  as part of https://github.com/Anthony17serrato/Phovo/issues/123
    private val photoLibraryObserver = object : NSObject(), PHPhotoLibraryChangeObserverProtocol {
        override fun photoLibraryDidChange(changeInstance: PHChange) {
            refreshPermissionsState()
        }
    }

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            refreshPermissionsState()
        }
        refreshPermissionsState()
    }

    private fun refreshPermissionsState() {
        // tryEmit is guaranteed when BufferOverflow.DROP_OLDEST is used
        _permissionEvents.tryEmit(Unit)
    }

    private fun fetchGalleryPermissionStatus(): GalleryPermissionsStatus {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        return when (status) {
            PHAuthorizationStatusAuthorized -> {
                PHPhotoLibrary.sharedPhotoLibrary().registerChangeObserver(photoLibraryObserver)
                GalleryPermissionsStatus(
                    permissionStatus = PermissionStatus.Granted,
                    isLimited = false
                )
            }
            PHAuthorizationStatusLimited -> {
                PHPhotoLibrary.sharedPhotoLibrary().registerChangeObserver(photoLibraryObserver)
                GalleryPermissionsStatus(
                    permissionStatus = PermissionStatus.Ungranted,
                    isLimited = true
                )
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.sharedPhotoLibrary().unregisterChangeObserver(photoLibraryObserver)
                GalleryPermissionsStatus(
                    permissionStatus = PermissionStatus.Ungranted,
                    isLimited = false
                )
            }
            else -> {
                PHPhotoLibrary.sharedPhotoLibrary().unregisterChangeObserver(photoLibraryObserver)
                GalleryPermissionsStatus(
                    permissionStatus = PermissionStatus.PermanentlyDenied,
                    isLimited = false
                )
            }
        }
    }

    @DelicatePermissionsApi
    override fun galleryPermissionStatus(): GalleryPermissionsStatus {
        val currentStatus = fetchGalleryPermissionStatus()
        return currentStatus
    }

    override fun observeGalleryPermissionStatus(): Flow<GalleryPermissionsStatus> {
        return _permissionEvents.map {
            fetchGalleryPermissionStatus()
        }
    }

    override suspend fun requestGalleryPermissions(): GalleryPermissionsStatus {
        val currentStatus = fetchGalleryPermissionStatus()
        if (currentStatus.permissionStatus == PermissionStatus.Granted) {
            refreshPermissionsState()
            return currentStatus
        }

        val updatedStatus = suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { _ ->
                if (continuation.isActive) {
                    continuation.resume(fetchGalleryPermissionStatus())
                }
            }
        }
        refreshPermissionsState()
        return updatedStatus
    }

    override fun openSystemPermissionSettings() {
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(
                url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null
            )
        }
    }
}
