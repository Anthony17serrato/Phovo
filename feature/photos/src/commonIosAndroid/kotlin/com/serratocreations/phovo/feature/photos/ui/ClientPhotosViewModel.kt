package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.domain.GetPhotosFeedWithThumbnailsUseCase
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.permissions.GalleryPermissionsStatus
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.permissions.annotations.DelicatePermissionsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientPhotosViewModel(
    getPhotosFeedWithThumbnailsUseCase: GetPhotosFeedWithThumbnailsUseCase,
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val permissionRepository: PermissionRepository,
    ioDispatcher: CoroutineDispatcher
) : PhotosViewModel(getPhotosFeedWithThumbnailsUseCase, ioDispatcher) {

    @OptIn(DelicatePermissionsApi::class)
    override val defaultUiState: PhotosUiState = run {
        val galleryPermissionStatus = permissionRepository.galleryPermissionStatus()

        PhotosUiState(
            shouldShowWelcomeBottomSheet = galleryPermissionStatus.shouldShowWelcomeBottomSheet(),
            callToActions = getGalleryCallToAction(galleryPermissionStatus)?.let { setOf(it) } ?: emptySet()
        )
    }

    init {
        combine(
            permissionRepository.observeGalleryPermissionStatus(),
            serverConfigRepository.observeServerConfig()
                .distinctUntilChanged()
        ) { galleryPermissionStatus, serverConfig ->
            _photosUiState.update { current ->
                val callToActions: Set<CallToAction> = buildSet {
                    getGalleryCallToAction(galleryPermissionStatus)?.let { add(it) }
                    if (serverConfig == null) add(COMPLETE_SETUP_CALL_TO_ACTION)
                }
                current.copy(
                    shouldShowWelcomeBottomSheet = galleryPermissionStatus.shouldShowWelcomeBottomSheet(),
                    callToActions = callToActions
                )
            }
        }.launchIn(viewModelScope)
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
                    priority = CallToActionPriority.High,
                    // Investigate if permission can be upgraded to non-limited outside of system settings
                    action = permissionRepository::openSystemPermissionSettings
                )
            }
            galleryPermissionStatus.isLimited && galleryPermissionStatus.permissionStatus == PermissionStatus.Ungranted -> {
                CallToAction(
                    actionTitle = "Limited library access",
                    actionDescription = "Phovo needs access to all of your photos and videos to back them up automatically. Tap to grant permissions.",
                    priority = CallToActionPriority.High,
                    // Investigate if permission can be upgraded to non-limited outside of system settings
                    action = permissionRepository::openSystemPermissionSettings
                )
            }
            galleryPermissionStatus.permissionStatus == PermissionStatus.Ungranted -> {
                CallToAction(
                    actionTitle = "Backups are disabled",
                    actionDescription = "Your device images are not backed up. Phovo is operating as a server-dashboard only. Tap to grant permissions.",
                    priority = CallToActionPriority.High,
                    action = ::requestGalleryPermissions
                )
            }
            galleryPermissionStatus.permissionStatus == PermissionStatus.PermanentlyDenied -> {
                CallToAction(
                    actionTitle = "Backups are disabled",
                    actionDescription = "Your device images are not backed up. Phovo is operating as a server-dashboard only. Tap to open Settings and allow access.",
                    priority = CallToActionPriority.High,
                    action = permissionRepository::openSystemPermissionSettings
                )
            }
            else -> {
                null
            }
        }
    }

    companion object {
        private val COMPLETE_SETUP_CALL_TO_ACTION = CallToAction(
            actionTitle = "Finish setup",
            actionDescription = "Get more from your gallery",
            priority = CallToActionPriority.Low,
            action = { /* TODO */ }
        )
    }
}