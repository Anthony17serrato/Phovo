package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.designsystem.component.PhovoNavOptions
import com.serratocreations.phovo.core.designsystem.component.PhovoRoute
import com.serratocreations.phovo.data.server.data.ConfigStatus
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ConnectionsViewModel(
    /** Only available on desktop clients. */
    private val serverConfigManager: ServerConfigManager
): ViewModel() {
    private val initialState = ConnectionsUiState(
        doesCurrentDeviceSupportServer = serverConfigManager is DesktopServerConfigManager,
    )
    private val _connectionsUiState = MutableStateFlow(initialState)
    val connectionsUiState = _connectionsUiState.asStateFlow()

    init {
        observeDeviceServerConfigurationState()
    }

    fun configureAsServer() {
        if (serverConfigManager is DesktopServerConfigManager) {
            // Todo safely handle null scenario(unlikely)
            _connectionsUiState.value.selectedDirectory?.let { directory ->
                serverConfigManager.configureDeviceAsServer(ServerConfig(directory))
            }
        }
    }

    private fun observeDeviceServerConfigurationState() {
        if (serverConfigManager is DesktopServerConfigManager) {
            serverConfigManager.observeDeviceServerConfigurationState(viewModelScope)
                .onEach { serverConfigState ->
                    val configStatus = serverConfigState.configStatus
                    val hostUrl = if (configStatus is ConfigStatus.Configured) {
                        configStatus.serverUrl
                    } else null
                    _connectionsUiState.update {
                        it.copy(
                            isCurrentDeviceServerConfigured = serverConfigState.configStatus is ConfigStatus.Configured,
                            serverEventLogs = serverConfigState.serverEventLogs,
                            hostUrl = hostUrl
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun navigateToPane(pane: PaneId, vararg options: PhovoNavOptions) {
        _connectionsUiState.update { currentPane ->
            val shouldNavigateBack = options.filterIsInstance<PhovoNavOptions.NavigateToBackstack>()
                .map { true }.firstOrNull() ?: false
            var previousPane = currentPane.currentConnectionsPane
            val newPane = if (shouldNavigateBack){
                while (previousPane.paneId != pane) {
                    previousPane = previousPane.previousPane ?: break
                }
                previousPane
            }else {
                when (pane) {
                    PaneId.Home -> ConnectionsPane.Home
                    PaneId.DefaultSecondPane -> ConnectionsPane.DefaultSecondPane
                    PaneId.ConfigGettingStarted -> ConnectionsPane.ConfigGettingStarted(previousPane)
                    PaneId.ConfigStorageSelection -> ConnectionsPane.ConfigStorageSelection(previousPane)
                }
            }
            currentPane.copy(currentConnectionsPane = newPane,)
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
                    currentConnectionsPane = previousPane,
                )
            } ?: currentUiState
        }
        return canNavigateBack
    }

    fun setSelectedDirectory(selectedDirectory: String) {
        _connectionsUiState.update { currentUiState ->
            currentUiState.copy(selectedDirectory = "$selectedDirectory/DO_NOT_DELETE_PHOVO",)
        }
    }
}

data class ConnectionsUiState(
    /** Determines if the current device has been configured as a Phovo server. */
    val isCurrentDeviceServerConfigured: Boolean = false,
    /**
     * Determines if the current device supports being configured as a Phovo server.
     * Only desktop clients support being configured as a Phovo server.
     */
    val doesCurrentDeviceSupportServer: Boolean = false,
    val serverEventLogs: List<String> = emptyList(),
    val currentConnectionsPane: ConnectionsPane = ConnectionsPane.Home,
    val selectedDirectory: String? = null,
    val hostUrl: String? = null
)

sealed class ConnectionsPane(
    open val previousPane: ConnectionsPane? = null,
    val paneId: PaneId
) {
    data object Home : ConnectionsPane(paneId = PaneId.Home)
    // Placeholder for the default second pane
    data object DefaultSecondPane : ConnectionsPane(paneId = PaneId.DefaultSecondPane)

    data class ConfigGettingStarted(
        override val previousPane: ConnectionsPane
    ) : ConnectionsPane(
        previousPane = previousPane,
        paneId = PaneId.ConfigGettingStarted
    )

    data class ConfigStorageSelection(
        override val previousPane: ConnectionsPane
    ) : ConnectionsPane(
        previousPane = previousPane,
        paneId = PaneId.ConfigStorageSelection
    )
}

enum class PaneId: PhovoRoute {
    Home,
    DefaultSecondPane,
    ConfigGettingStarted,
    ConfigStorageSelection
}