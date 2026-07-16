package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.DiscoveredServer
import com.serratocreations.phovo.data.server.ServerDiscoveryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientConnectionsViewModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val serverDiscoveryManager: ServerDiscoveryManager
): ConnectionsViewModel(
    serverConfigRepository = serverConfigRepository
) {
    private var discoveryJob: Job? = null
    override val initialState: ClientConnectionsUiState = ClientConnectionsUiState()

    private val _connectionsUiState: MutableStateFlow<ClientConnectionsUiState> = MutableStateFlow(initialState)
    override val connectionsUiState = _connectionsUiState.asStateFlow()

    init {
        observeClientConfigState()
    }

    private fun observeClientConfigState() {
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

    override fun startDiscovery() {
        if (discoveryJob == null) {
            _connectionsUiState.update { it.copy(isSearching = true) }
            discoveryJob = serverDiscoveryManager.discoverServers()
                .onEach { servers ->
                    _connectionsUiState.update { it.copy(discoveredServers = servers) }
                }
                .launchIn(viewModelScope)
        }
    }

    override fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _connectionsUiState.update { it.copy(isSearching = false, discoveredServers = emptyList()) }
    }

    override fun connectToServer(server: DiscoveredServer) {
        viewModelScope.launch {
            serverDiscoveryManager.connectToServer(server)
        }
    }

    override fun connectManually(url: String) {
        viewModelScope.launch {
            serverConfigRepository.updateClientServerConfig(url)
        }
    }

    override fun disconnectFromServer() {
        viewModelScope.launch {
            serverConfigRepository.clearClientServerConfig()
        }
    }
}

data class ClientConnectionsUiState(
    override val isClientConfigured: Boolean = false,
    override val configuredServerUrl: String? = null,
    override val isSearching: Boolean = false,
    override val discoveredServers: List<DiscoveredServer> = emptyList()
): ConnectionsUiState