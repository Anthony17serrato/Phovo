package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.common.di.IoDispatcher
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.UriPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.toPhotoUiItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.annotation.KoinViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class PhotosViewModel(
    private val mediaRepository: MediaRepository,
    private val serverConfigRepository: ServerConfigRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {
    private val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    private val _photosUiState = MutableStateFlow(PhotosUiState())
    val photosUiState = _photosUiState.asStateFlow()

    init {
        if(mediaRepository is LocalSupportMediaRepository){
            mediaRepository.initMediaProcessing()
        }

        serverConfigRepository.observeServerConfig().map { it?.backupDirectory }
            .distinctUntilChanged().mapLatest { localDirectory ->
                mediaRepository.phovoItemsFlow(localDirectory).collect { phovoItems ->
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