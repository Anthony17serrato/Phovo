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
open class PhotosViewModel(
    getPhotosFeedWithThumbnailsUseCase: GetPhotosFeedWithThumbnailsUseCase,
    ioDispatcher: CoroutineDispatcher
): ViewModel() {

    protected open val defaultUiState = PhotosUiState(shouldShowWelcomeBottomSheet = false)
    @OptIn(ExperimentalTime::class)
    private val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    protected val _photosUiState = MutableStateFlow(PhotosUiState(shouldShowWelcomeBottomSheet = false))
    val photosUiState = _photosUiState.asStateFlow()

    init {
        getPhotosFeedWithThumbnailsUseCase()
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

    open fun onProceedWelcomeBottomSheet() {

    }

    fun onPhotoSelected(mediaUiItem: MediaUiItem) {
        _photosUiState.update { currentState ->
            currentState.copy(selectedPhoto = mediaUiItem)
        }
    }
}

data class PhotosUiState(
    val photosFeed: List<PhotoUiItem> = emptyList(),
    val selectedPhoto: MediaUiItem? = null,
    val shouldShowWelcomeBottomSheet: Boolean,
    // TODO UI State should support displaying multiple call to action components
    //  in a carousel manner.
    val callToAction: CallToAction? = CallToAction(
        actionTitle = "Finish setup",
        actionDescription = "Get more from your gallery",
        action = { /* TODO */ }
    )
)

data class CallToAction(
    val actionTitle: String,
    val actionDescription: String,
    val action: () -> Unit
)