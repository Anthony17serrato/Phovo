package com.serratocreations.phovo.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.Foundation.NSURL

class IosPermissionManager : PermissionManager {
    override fun getPermissionState(activity: Any?): PermissionState {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        return when (status) {
            PHAuthorizationStatusAuthorized -> PermissionState.Granted
            PHAuthorizationStatusLimited -> PermissionState.Limited
            PHAuthorizationStatusNotDetermined -> PermissionState.NotDetermined
            PHAuthorizationStatusDenied,
            PHAuthorizationStatusRestricted -> PermissionState.Denied
            else -> PermissionState.Denied
        }
    }

    override fun openSettings() {
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
@Composable
actual fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester {
    return remember {
        object : PermissionRequester {
            override fun requestPermission() {
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { status ->
                    val isGranted = status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
                    // Dispatch to Main thread to ensure UI-thread safety
                    platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                        onResult(isGranted)
                    }
                }
            }
        }
    }
}

@Composable
actual fun getActivity(): Any? = null
