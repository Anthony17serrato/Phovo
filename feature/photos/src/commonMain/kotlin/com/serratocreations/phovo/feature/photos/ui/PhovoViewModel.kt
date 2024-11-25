package com.serratocreations.phovo.feature.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.feature.photos.data.repository.PhovoItemRepository
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.toImagePhotoUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Month
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PhovoViewModel(
    private val phovoItemRepository: PhovoItemRepository
): ViewModel() {
    private val _phovoUiState = MutableStateFlow(emptyList<PhotoUiItem>())
    val phovoUiState = _phovoUiState.asStateFlow()

    init {
        viewModelScope.launch {
            phovoItemRepository.phovoItemsFlow().collect { phovoItems ->
                val uiItemList = mutableListOf<PhotoUiItem>()
                phovoItems.forEachIndexed { index, phovoItem ->
                    if (index == 0) {
                        uiItemList.add(DateHeaderPhotoUiItem(Month.NOVEMBER, ""))
                    }
                    uiItemList.add(phovoItem.toImagePhotoUiItem())
                    if ((index != 0) && (index % 8 == 0)) {
                        uiItemList.add(DateHeaderPhotoUiItem(Month.OCTOBER, ""))
                    }
                }
                _phovoUiState.update {
                    uiItemList
                }
            }
        }
    }

    override fun onCleared() {
        // TODO some cleanup
        super.onCleared()
    }
}