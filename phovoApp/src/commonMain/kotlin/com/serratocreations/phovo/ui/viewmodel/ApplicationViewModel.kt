package com.serratocreations.phovo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepository
import com.serratocreations.phovo.ui.model.OverflowMenuOption
import com.serratocreations.phovo.util.getFlavorOptions
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
    private val _applicationUiState = MutableStateFlow<ApplicationUiState>(
        ApplicationUiState(menuOptions = getOverflowMenuOptions())
    )
    val applicationUiState = _applicationUiState.asStateFlow()

    init {
        viewModelScope.observeServerStatus()
    }

    private fun CoroutineScope.observeServerStatus() = launch {
        if(mediaRepository is RemoteMediaRepository) {
            mediaRepository.observeServerConnection()
                .onEach { isServerConnectionSuccess ->
                    _applicationUiState.update { uiState ->
                        uiState.copy(serverStatusColor = if (isServerConnectionSuccess) {
                                ServerStatusColor.Green
                            } else {
                                ServerStatusColor.Red
                            }
                        )
                    }
                }
                .launchIn(this)
        }
    }

    private fun getOverflowMenuOptions(): Set<OverflowMenuOption> {
        val optionsSet = mutableSetOf<OverflowMenuOption>()
        optionsSet.addAll(getFlavorOptions())
        // Add common options here
        return optionsSet.toSet()
    }
}

data class ApplicationUiState(
    val serverStatusColor: ServerStatusColor = ServerStatusColor.Unavailable,
    val menuOptions: Set<OverflowMenuOption>
)

sealed interface ServerStatusColor {
    data object Unavailable: ServerStatusColor
    data object Green: ServerStatusColor
    data object Red: ServerStatusColor
}