package com.serratocreations.phovo.feature.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.feature.photos.data.db.entity.PhovoItem
import com.serratocreations.phovo.feature.photos.data.repository.PhovoItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PhovoViewModel(
    private val phovoItemRepository: PhovoItemRepository
): ViewModel() {
    private val _phovoUiState = MutableStateFlow(emptyList<PhovoItem>())
    val phovoUiState = _phovoUiState.asStateFlow()

    init {
        viewModelScope.launch {
            phovoItemRepository.phovoItemsFlow().collect { phovoItems ->
                _phovoUiState.update {
                    phovoItems
                }
            }
        }
    }

    override fun onCleared() {
        // TODO some cleanup
        super.onCleared()
    }
}