package com.serratocreations.phovo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.repository.PhovoItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PhovoViewModel: ViewModel() {
    private val _kanbanUiState = MutableStateFlow(emptyList<String>())
    val kanbanUiState = _kanbanUiState.asStateFlow()

    init {
        viewModelScope.launch {
            PhovoItemRepository.phovoItemsFlow().map {
                it.map { item -> item.title }
            }.collect { kanbanItems ->
                _kanbanUiState.update {
                    kanbanItems
                }
            }
        }
    }
    suspend fun getPhovoItems() = PhovoItemRepository.phovoItemsFlow().map {
        it.first().title
    }.first()

    override fun onCleared() {
        // TODO some cleanup
        super.onCleared()
    }
}