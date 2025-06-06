package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.photos.repository.PhovoItemRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.toImagePhotoUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PhotosViewModel(
    private val phovoItemRepository: PhovoItemRepository,
    private val serverConfigRepository: ServerConfigRepository
): ViewModel() {
    private val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    private val _phovoUiState = MutableStateFlow(emptyList<PhotoUiItem>())
    val phovoUiState = _phovoUiState.asStateFlow()

    init {
        viewModelScope.launch {
            serverConfigRepository.observeServerConfig().map { it?.backupDirectory }
                .distinctUntilChanged().collectLatest { localDirectory ->
                    phovoItemRepository.phovoItemsFlow(localDirectory).collect { phovoItems ->
                        val uiItemList = mutableListOf<PhotoUiItem>()
                        phovoItems.groupBy { Pair(it.dateInFeed.month, it.dateInFeed.year) }.forEach { entry ->
                            uiItemList.add(
                                DateHeaderPhotoUiItem(
                                    month = entry.key.first,
                                    year = entry.key.second.takeIf { it != currentYear }
                                )
                            )
                            uiItemList.addAll(entry.value.map { it.toImagePhotoUiItem() })
                        }
                        _phovoUiState.update {
                            uiItemList
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        // TODO some cleanup
        super.onCleared()
    }
}