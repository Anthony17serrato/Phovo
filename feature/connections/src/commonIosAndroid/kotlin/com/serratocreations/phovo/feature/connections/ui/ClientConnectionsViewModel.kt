package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.server.ServerDiscoveryManager
import com.serratocreations.phovo.data.server.data.model.DiscoveredServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientConnectionsViewModel(
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val serverDiscoveryManager: ServerDiscoveryManager,
    private val permissionRepository: PermissionRepository
): ConnectionsViewModel(
    serverConfigRepository = serverConfigRepository
) {
    private var discoveryJob: Job? = null
    override val initialState: ClientConnectionsUiState = ClientConnectionsUiState()

    private val _connectionsUiState: MutableStateFlow<ClientConnectionsUiState> = MutableStateFlow(initialState)
    override val connectionsUiState = _connectionsUiState.asStateFlow()

    init {
        observeClientConfigState()
        observePermissionStatus()
    }

    private fun observeClientConfigState() {
        serverConfigRepository.observeServerConfig()
            .onEach { serverConfig ->
                val isConfigured = serverConfig != null
                val serverUrl = serverConfig?.serverBaseUrlString?.value

                _connectionsUiState.update {
                    it.copy(
                        isClientConfigured = isConfigured,
                        configuredServerUrl = serverUrl
                    )
                }

                if (isConfigured) {
                    stopDiscovery()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observePermissionStatus() {
        permissionRepository.observeLocalNetworkPermissionStatus()
            .onEach { status ->
                val isGranted = status == PermissionStatus.Granted
                _connectionsUiState.update { currentState ->
                    currentState.copy(localNetworkPermissionStatus = status)
                }
                if (isGranted && _connectionsUiState.value.isClientConfigured.not()) {
                    startDiscovery()
                }
            }
            .launchIn(viewModelScope)
    }

    fun requestLocalNetworkPermission() {
        viewModelScope.launch {
            val result = permissionRepository.requestLocalNetworkPermissions()
            val isGranted = result == PermissionStatus.Granted
            _connectionsUiState.update {
                it.copy(localNetworkPermissionStatus = result)
            }
            if (isGranted && !_connectionsUiState.value.isClientConfigured) {
                startDiscovery()
            }
        }
    }

    fun toggleManualUrlExpanded() {
        _connectionsUiState.update { currentState ->
            currentState.copy(isManualUrlExpanded = !currentState.isManualUrlExpanded)
        }
    }

    fun openPermissionSettings() {
        permissionRepository.openSystemPermissionSettings()
    }

    fun startDiscovery() {
        if (discoveryJob == null) {
            _connectionsUiState.update { it.copy(isSearching = true) }
            discoveryJob = serverDiscoveryManager.discoverServers()
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
}

data class ClientConnectionsUiState(
    val isClientConfigured: Boolean = false,
    val configuredServerUrl: String? = null,
    val isSearching: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val localNetworkPermissionStatus: PermissionStatus = PermissionStatus.Ungranted,
    val isManualUrlExpanded: Boolean = false
): ConnectionsUiState