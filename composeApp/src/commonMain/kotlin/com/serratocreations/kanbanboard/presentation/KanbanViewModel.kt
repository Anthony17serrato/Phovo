package com.serratocreations.kanbanboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.kanbanboard.data.repository.KanbanItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class KanbanViewModel: ViewModel() {
    private val _kanbanUiState = MutableStateFlow(emptyList<String>())
    val kanbanUiState = _kanbanUiState.asStateFlow()

    init {
        viewModelScope.launch {
            KanbanItemRepository.kanbanItemsFlow().map {
                it.map { item -> item.title }
            }.collect { kanbanItems ->
                _kanbanUiState.update {
                    kanbanItems
                }
            }
        }
    }
    suspend fun getKanbanItems() = KanbanItemRepository.kanbanItemsFlow().map {
        it.first().title
    }.first()

    override fun onCleared() {
        // TODO some cleanup
        super.onCleared()
    }
}