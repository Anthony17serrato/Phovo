package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.UriPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.toPhotoUiItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class PhotosViewModel(
    mediaRepository: MediaRepository,
    ioDispatcher: CoroutineDispatcher
): ViewModel() {
    @OptIn(ExperimentalTime::class)
    private val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    private val _photosUiState = MutableStateFlow(PhotosUiState())
    val photosUiState = _photosUiState.asStateFlow()

    init {
        mediaRepository.phovoMediaFlow().onEach { phovoItems ->
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

    fun onPhotoClick(uriPhotoUiItem: UriPhotoUiItem) {
        _photosUiState.update { currentState ->
            currentState.copy(selectedPhoto = uriPhotoUiItem)
        }
    }
}

data class PhotosUiState(
    val photosFeed: List<PhotoUiItem> = emptyList(),
    val selectedPhoto: UriPhotoUiItem? = null
)