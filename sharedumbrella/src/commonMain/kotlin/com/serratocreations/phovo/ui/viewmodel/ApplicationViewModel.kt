package com.serratocreations.phovo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.model.network.ServerConnectionState
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

    private fun CoroutineScope.observeServerStatus() {
        if (mediaRepository !is RemoteMediaRepository) return
        mediaRepository.observeConnectionState()
            .onEach { connectionState ->
                val statusColor = when (connectionState) {
                    is ServerConnectionState.Connected -> ServerStatusColor.Green
                    // A mismatch needs attention as much as an outage: the server the user paired
                    // with is not the one answering.
                    is ServerConnectionState.Unreachable,
                    ServerConnectionState.IdentityMismatch -> ServerStatusColor.Red
                    // No server configured, or the first check has not landed. Red here would
                    // report a problem the user does not have.
                    ServerConnectionState.Unknown,
                    ServerConnectionState.Checking -> ServerStatusColor.Unavailable
                }
                _applicationUiState.update { uiState ->
                    uiState.copy(serverStatusColor = statusColor)
                }
            }
            .launchIn(this)
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