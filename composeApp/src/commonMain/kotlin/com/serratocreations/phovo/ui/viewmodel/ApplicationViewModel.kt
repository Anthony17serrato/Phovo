package com.serratocreations.phovo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApplicationViewModel(
    private val mediaRepository: MediaRepository
): ViewModel() {
    private val _applicationUiState = MutableStateFlow<ServerStatusColor>(Unavailable)
    val applicationUiState = _applicationUiState.asStateFlow()

    init {
        viewModelScope.observeServerStatus()
    }

    private fun CoroutineScope.observeServerStatus() = launch {
        if(mediaRepository is RemoteMediaRepository) {
            mediaRepository.observeServerConnection()
                .onEach { isServerConnectionSuccess ->
                    _applicationUiState.update { uiState ->
                        if (isServerConnectionSuccess) {
                            Green
                        } else { Red }
                    }
                }
                .launchIn(this)
        }
    }
}

sealed interface ServerStatusColor

data object Unavailable: ServerStatusColor
data object Green: ServerStatusColor
data object Red: ServerStatusColor