package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.domain.GetPhotosFeedWithThumbnailsUseCase
import com.serratocreations.phovo.data.permissions.GalleryPermissionsStatus
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.permissions.annotations.DelicatePermissionsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientPhotosViewModel(
    getPhotosFeedWithThumbnailsUseCase: GetPhotosFeedWithThumbnailsUseCase,
    private val permissionRepository: PermissionRepository,
    ioDispatcher: CoroutineDispatcher
) : PhotosViewModel(getPhotosFeedWithThumbnailsUseCase, ioDispatcher) {

    @OptIn(DelicatePermissionsApi::class)
    override val defaultUiState: PhotosUiState = run {
        val galleryPermissionStatus = permissionRepository.galleryPermissionStatus()

        PhotosUiState(
            shouldShowWelcomeBottomSheet = galleryPermissionStatus.shouldShowWelcomeBottomSheet(),
            callToAction = getGalleryCallToAction(galleryPermissionStatus)
        )
    }

    init {
        permissionRepository.observeGalleryPermissionStatus()
            .onEach { galleryPermissionStatus ->
                _photosUiState.update { current ->
                    current.copy(
                        shouldShowWelcomeBottomSheet = galleryPermissionStatus.shouldShowWelcomeBottomSheet(),
                        callToAction = getGalleryCallToAction(galleryPermissionStatus)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onProceedWelcomeBottomSheet() {
        super.onProceedWelcomeBottomSheet()

        requestGalleryPermissions()
    }

    private fun requestGalleryPermissions() {
        viewModelScope.launch {
            permissionRepository.requestGalleryPermissions()
        }
    }

    private fun GalleryPermissionsStatus.shouldShowWelcomeBottomSheet() =
        this.permissionStatus == PermissionStatus.Ungranted && this.isLimited.not()

    private fun getGalleryCallToAction(galleryPermissionStatus: GalleryPermissionsStatus): CallToAction? {
        return when {
            galleryPermissionStatus.isLimited && galleryPermissionStatus.permissionStatus == PermissionStatus.PermanentlyDenied -> {
                CallToAction(
                    actionTitle = "Limited library access",
                    actionDescription = "Phovo needs access to all of your photos and videos to back them up automatically. Tap to open Settings and allow full access.",
                    // Investigate if permission can be upgraded to non-limited outside of system settings
                    action = permissionRepository::openSystemPermissionSettings
                )
            }
            galleryPermissionStatus.isLimited && galleryPermissionStatus.permissionStatus == PermissionStatus.Ungranted -> {
                CallToAction(
                    actionTitle = "Limited library access",
                    actionDescription = "Phovo needs access to all of your photos and videos to back them up automatically. Tap to grant permissions.",
                    // Investigate if permission can be upgraded to non-limited outside of system settings
                    action = permissionRepository::openSystemPermissionSettings
                )
            }
            galleryPermissionStatus.permissionStatus == PermissionStatus.Ungranted -> {
                CallToAction(
                    actionTitle = "Backups are disabled",
                    actionDescription = "Your device images are not backed up. Phovo is operating as a server-dashboard only. Tap to grant permissions.",
                    action = ::requestGalleryPermissions
                )
            }
            galleryPermissionStatus.permissionStatus == PermissionStatus.PermanentlyDenied -> {
                CallToAction(
                    actionTitle = "Backups are disabled",
                    actionDescription = "Your device images are not backed up. Phovo is operating as a server-dashboard only. Tap to open Settings and allow access.",
                    action = permissionRepository::openSystemPermissionSettings
                )
            }
            else -> {
                null
            }
        }
    }
}