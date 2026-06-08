package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.server.data.ConfigStatus
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.core.model.ServerConfig
import io.github.vinceglb.filekit.PlatformFile
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
                serverConfigManager.configureDeviceAsServer(ServerConfig.ServerSpecificServerConfig(directory))
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

    fun setSelectedDirectory(selectedDirectory: PlatformFile) {
        _connectionsUiState.update { currentUiState ->
            currentUiState.copy(selectedDirectory = selectedDirectory)
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
    val selectedDirectory: PlatformFile? = null,
    val hostUrl: String? = null
)