package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.feature.connections.data.ConfigStatus
import com.serratocreations.phovo.feature.connections.data.DesktopServerConfigManager
import com.serratocreations.phovo.feature.connections.data.ServerConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ConnectionsViewModel(
    /** Only available on desktop clients. */
    private val serverConfigManager: ServerConfigManager
): ViewModel() {
    private val initialState = ConnectionsUiState(
        doesCurrentDeviceSupportServer = serverConfigManager is DesktopServerConfigManager
    )
    private val _connectionsUiState = MutableStateFlow(initialState)
    val connectionsUiState = _connectionsUiState.asStateFlow()

    init {
        observeDeviceServerConfigurationState()
    }

    fun configureAsServer() {
        if (serverConfigManager is DesktopServerConfigManager) {
            serverConfigManager.configureDeviceAsServer()
        }
    }

    private fun observeDeviceServerConfigurationState() {
        if (serverConfigManager is DesktopServerConfigManager) {
            serverConfigManager.observeDeviceServerConfigurationState(viewModelScope)
                .onEach { serverConfigState ->
                    _connectionsUiState.update {
                        it.copy(
                            isCurrentDeviceServerConfigured = serverConfigState.configStatus !is ConfigStatus.NotConfigured,
                            serverEventLogs = serverConfigState.serverEventLogs
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun showConfigPane() {
        _connectionsUiState.update {
            it.copy(
                currentConnectionsPane = ConnectionsPane.ConfigGettingStarted(it.currentConnectionsPane)
            )
        }
    }

    /**
     * @return [Boolean] indicating if there are any remaining panes to navigate back to.
     */
    fun onBackClick(): Boolean {
        var canNavigateBack = false
        _connectionsUiState.update { currentUiState ->
            currentUiState.currentConnectionsPane.previousPane?.let { previousPane ->
                canNavigateBack = previousPane.previousPane != null
                currentUiState.copy(
                    currentConnectionsPane = previousPane
                )
            } ?: currentUiState
        }
        return canNavigateBack
    }
}

data class ConnectionsUiState(
    /** Determines if the current device has been configured as a Phovo server. */
    val isCurrentDeviceServerConfigured: Boolean = true,
    /**
     * Determines if the current device supports being configured as a Phovo server.
     * Only desktop clients support being configured as a Phovo server.
     */
    val doesCurrentDeviceSupportServer: Boolean = false,
    val serverEventLogs: List<String> = emptyList(),
    val currentConnectionsPane: ConnectionsPane = ConnectionsPane.Home
)

sealed class ConnectionsPane(open val previousPane: ConnectionsPane? = null) {
    data object Home : ConnectionsPane()
    // Placeholder for the default second pane
    data object DefaultSecondPane : ConnectionsPane()
    data class ConfigGettingStarted(override val previousPane: ConnectionsPane) : ConnectionsPane(previousPane)
}