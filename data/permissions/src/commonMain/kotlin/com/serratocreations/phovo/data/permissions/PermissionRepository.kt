package com.serratocreations.phovo.data.permissions

import com.serratocreations.phovo.data.permissions.annotations.DelicatePermissionsApi
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    @DelicatePermissionsApi
    fun galleryPermissionStatus(): GalleryPermissionsStatus

    fun observeGalleryPermissionStatus(): Flow<GalleryPermissionsStatus>

    suspend fun requestGalleryPermissions(): GalleryPermissionsStatus

    fun openSystemPermissionSettings()
}

data class GalleryPermissionsStatus(
    val permissionStatus: PermissionStatus,
    val isLimited: Boolean
)

enum class PermissionStatus {
    Granted,
    PermanentlyDenied,
    Ungranted
}