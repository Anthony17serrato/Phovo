package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.data.server.data.ConfigStatus
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.discovery.ServerDiscoveryManager
import com.serratocreations.phovo.core.serverconfig.discovery.DiscoveredServer
import com.serratocreations.phovo.core.designsystem.component.PhovoRoute
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectionsViewModel(
    /** Only available on desktop clients. */
    private val serverConfigManager: ServerConfigManager,
    private val serverConfigRepository: ServerConfigRepository,
    private val serverDiscoveryManager: ServerDiscoveryManager
): ViewModel() {
    private val initialState = ConnectionsUiState(
        doesCurrentDeviceSupportServer = serverConfigManager is DesktopServerConfigManager,
    )
    private val _connectionsUiState = MutableStateFlow(initialState)
    val connectionsUiState = _connectionsUiState.asStateFlow()

    private var discoveryJob: Job? = null

    init {
        observeDeviceServerConfigurationState()
        observeClientConfigState()
    }

    private fun observeClientConfigState() {
        if (!initialState.doesCurrentDeviceSupportServer) {
            serverConfigRepository.observeServerConfig()
                .onEach { serverConfig ->
                    val clientConfig = serverConfig as? ServerConfig.ClientSpecificServerConfig
                    val isConfigured = clientConfig != null
                    val serverUrl = clientConfig?.serverBaseUrlString?.value

                    _connectionsUiState.update {
                        it.copy(
                            isClientConfigured = isConfigured,
                            configuredServerUrl = serverUrl
                        )
                    }

                    if (isConfigured) {
                        stopDiscovery()
                    } else {
                        startDiscovery()
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun startDiscovery() {
        if (discoveryJob == null) {
            _connectionsUiState.update { it.copy(isSearching = true) }
            discoveryJob = serverDiscoveryManager.startDiscovery()
                .onEach { servers ->
                    _connectionsUiState.update { it.copy(discoveredServers = servers) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _connectionsUiState.update { it.copy(isSearching = false, discoveredServers = emptyList()) }
        serverDiscoveryManager.stopDiscovery()
    }

    fun connectToServer(server: DiscoveredServer) {
        viewModelScope.launch {
            serverDiscoveryManager.connectToServer(server)
        }
    }

    fun connectManually(url: String) {
        viewModelScope.launch {
            serverConfigRepository.updateClientServerConfig(url)
        }
    }

    fun disconnectFromServer() {
        viewModelScope.launch {
            serverConfigRepository.clearClientServerConfig()
        }
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
    val currentConnectionsPane: ConnectionsPane = ConnectionsPane.Home,
    val selectedDirectory: PlatformFile? = null,
    val hostUrl: String? = null,
    val isClientConfigured: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val isSearching: Boolean = false,
    val configuredServerUrl: String? = null
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