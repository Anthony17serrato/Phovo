package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.viewModelScope
import com.serratocreations.phovo.core.model.ServerConfig
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.data.ConfigStatus
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ServerConnectionsViewModel(
    private val serverConfigManager: DesktopServerConfigManager,
    private val serverConfigRepository: ServerConfigRepository,
): ConnectionsViewModel(
    serverConfigRepository = serverConfigRepository
) {
    override val initialState: ServerConnectionsUiState = run {
        val serverName = serverConfigManager.getDefaultServerName()
        ServerConnectionsUiState(
            defaultServerName = serverName,
            serverName = serverName
        )
    }
    private val _connectionsUiState: MutableStateFlow<ServerConnectionsUiState> = MutableStateFlow(initialState)
    override val connectionsUiState = _connectionsUiState.asStateFlow()

    init {
        observeDeviceServerConfigurationState()
    }

    override fun configureAsServer() {
        // Todo safely handle null scenario(unlikely)
        _connectionsUiState.value.selectedDirectory?.let { directory ->
            val name = _connectionsUiState.value.serverName.takeIf { it.isNotBlank() }
                ?: _connectionsUiState.value.defaultServerName.takeIf { it.isNotBlank() }
                ?: "Phovo Server"
            serverConfigManager.configureDeviceAsServer(
                ServerConfig.ServerSpecificServerConfig(
                    backupDirectory = directory,
                    serverName = name
                )
            )
        }
    }

    private fun observeDeviceServerConfigurationState() {
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

    override fun setServerName(name: String) {
        _connectionsUiState.update { it.copy(serverName = name) }
    }

    override fun setSelectedDirectory(selectedDirectory: PlatformFile) {
        _connectionsUiState.update { currentUiState ->
            currentUiState.copy(selectedDirectory = selectedDirectory)
        }
    }
}

data class ServerConnectionsUiState(
    override val doesCurrentDeviceSupportServer: Boolean = true,
    override val defaultServerName: String,
    override val serverName: String,
    override val hostUrl: String? = null,
    override val isCurrentDeviceServerConfigured: Boolean = false,
    override val serverEventLogs: List<String> = emptyList(),
    override val selectedDirectory: PlatformFile? = null
): ConnectionsUiState