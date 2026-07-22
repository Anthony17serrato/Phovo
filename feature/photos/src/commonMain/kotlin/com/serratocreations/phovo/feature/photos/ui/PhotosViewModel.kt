package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.domain.GetPhotosFeedWithThumbnailsUseCase
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.MediaUiItem
import com.serratocreations.phovo.feature.photos.mappers.toPhotoUiItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

import com.serratocreations.phovo.core.common.PermissionManager
import com.serratocreations.phovo.core.common.PermissionState
import com.serratocreations.phovo.core.common.LocalMediaSyncTrigger

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PhotosViewModel(
    getPhotosFeedWithThumbnailsUseCase: GetPhotosFeedWithThumbnailsUseCase,
    private val permissionManager: PermissionManager,
    private val localMediaSyncTrigger: LocalMediaSyncTrigger,
    ioDispatcher: CoroutineDispatcher
): ViewModel() {
    @OptIn(ExperimentalTime::class)
    private val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    private val _photosUiState = MutableStateFlow(PhotosUiState())
    val photosUiState = _photosUiState.asStateFlow()

    init {
        checkPermissionState()
        getPhotosFeedWithThumbnailsUseCase()
            .sample(500.milliseconds)
            .onEach { phovoItems ->
                val uiItemList = mutableListOf<PhotoUiItem>()
                phovoItems.groupBy { Pair(it.dateInFeed.month, it.dateInFeed.year) }.forEach { entry ->
                    uiItemList.add(
                        DateHeaderPhotoUiItem(
                            month = entry.key.first,
                            year = entry.key.second.takeIf { it != currentYear }
                        )
                    )
                    uiItemList.addAll(entry.value.map { it.toPhotoUiItem() })
                }
                _photosUiState.update { currentState ->
                    currentState.copy(photosFeed = uiItemList)
                }
            }.flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        // TODO some cleanup
        super.onCleared()
    }

    fun onPhotoSelected(mediaUiItem: MediaUiItem) {
        _photosUiState.update { currentState ->
            currentState.copy(selectedPhoto = mediaUiItem)
        }
    }

    fun checkPermissionState(activity: Any? = null) {
        val state = permissionManager.getPermissionState(activity)
        _photosUiState.update { currentState ->
            currentState.copy(permissionState = state)
        }
        // If permission becomes Granted or Limited, trigger background media processing
        if (state == PermissionState.Granted || state == PermissionState.Limited) {
            initLocalMediaSync()
        }
    }

    fun initLocalMediaSync() {
        localMediaSyncTrigger.triggerSync()
    }

    fun openSettings() {
        permissionManager.openSettings()
    }
}

data class PhotosUiState(
    val photosFeed: List<PhotoUiItem> = emptyList(),
    val selectedPhoto: MediaUiItem? = null,
    val permissionState: PermissionState = PermissionState.NotDetermined
)