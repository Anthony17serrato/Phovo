package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
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

enum class OnboardingStep {
    Welcome,
    PermissionPrimer,
    ServerDiscovery
}

class ClientConnectionsViewModel(
    private val serverConfigRepository: ServerConfigRepository,
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
                } else if (_connectionsUiState.value.currentStep == OnboardingStep.ServerDiscovery) {
                    startDiscovery()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observePermissionStatus() {
        permissionRepository.observeLocalNetworkPermissionStatus()
            .onEach { status ->
                val isGranted = status == PermissionStatus.Granted
                val isPermanentlyDenied = status == PermissionStatus.PermanentlyDenied
                _connectionsUiState.update { currentState ->
                    val nextStep = if (isGranted && currentState.currentStep != OnboardingStep.Welcome) {
                        OnboardingStep.ServerDiscovery
                    } else {
                        currentState.currentStep
                    }
                    currentState.copy(
                        localNetworkPermissionStatus = status,
                        isLocalNetworkPermissionGranted = isGranted,
                        isLocalNetworkPermissionPermanentlyDenied = isPermanentlyDenied,
                        currentStep = nextStep
                    )
                }
                if (isGranted && !_connectionsUiState.value.isClientConfigured) {
                    startDiscovery()
                }
            }
            .launchIn(viewModelScope)
    }

    fun nextStep() {
        _connectionsUiState.update { current ->
            val nextStep = when (current.currentStep) {
                OnboardingStep.Welcome -> {
                    if (current.isLocalNetworkPermissionGranted) {
                        OnboardingStep.ServerDiscovery
                    } else {
                        OnboardingStep.PermissionPrimer
                    }
                }
                OnboardingStep.PermissionPrimer -> OnboardingStep.ServerDiscovery
                OnboardingStep.ServerDiscovery -> OnboardingStep.ServerDiscovery
            }
            current.copy(currentStep = nextStep)
        }
        if (_connectionsUiState.value.currentStep == OnboardingStep.ServerDiscovery) {
            startDiscovery()
        }
    }

    fun previousStep() {
        _connectionsUiState.update { current ->
            val prevStep = when (current.currentStep) {
                OnboardingStep.Welcome -> OnboardingStep.Welcome
                OnboardingStep.PermissionPrimer -> OnboardingStep.Welcome
                OnboardingStep.ServerDiscovery -> OnboardingStep.PermissionPrimer
            }
            current.copy(currentStep = prevStep)
        }
    }

    fun requestLocalNetworkPermission() {
        viewModelScope.launch {
            val result = permissionRepository.requestLocalNetworkPermissions()
            val isGranted = result == PermissionStatus.Granted
            _connectionsUiState.update {
                it.copy(
                    localNetworkPermissionStatus = result,
                    isLocalNetworkPermissionGranted = isGranted,
                    currentStep = OnboardingStep.ServerDiscovery
                )
            }
            if (isGranted && !_connectionsUiState.value.isClientConfigured) {
                startDiscovery()
            }
        }
    }

    fun toggleManualUrlExpanded() {
        _connectionsUiState.update {
            it.copy(isManualUrlExpanded = !it.isManualUrlExpanded)
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
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val localNetworkPermissionStatus: PermissionStatus = PermissionStatus.Ungranted,
    val isLocalNetworkPermissionGranted: Boolean = false,
    val isLocalNetworkPermissionPermanentlyDenied: Boolean = false,
    val isManualUrlExpanded: Boolean = false
): ConnectionsUiState