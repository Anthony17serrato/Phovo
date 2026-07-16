package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.ViewModel
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.DiscoveredServer
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectionsViewModel(
    private val serverConfigRepository: ServerConfigRepository
): ViewModel() {
    protected abstract val initialState: ConnectionsUiState

    abstract val connectionsUiState: StateFlow<ConnectionsUiState>

    open fun startDiscovery() { }

    open fun stopDiscovery() { }

    open fun connectToServer(server: DiscoveredServer) { }

    open fun connectManually(url: String) { }

    open fun disconnectFromServer() { }

    // TODO Move into ServerConnectionsViewModel not needed for client
    open fun configureAsServer() {

    }

    open fun setServerName(name: String) {

    }

    open fun setSelectedDirectory(selectedDirectory: PlatformFile) {

    }
}

/**
 * Connections UI data model which contains properties that
 * are common for both client & server UI State.
 */
interface ConnectionsUiState {
    /** Determines if the current device has been configured as a Phovo server. */
    val isCurrentDeviceServerConfigured: Boolean
        get() = false

    /**
     * Determines if the current device supports being configured as a Phovo server.
     * Only desktop clients support being configured as a Phovo server.
     */
    val doesCurrentDeviceSupportServer: Boolean
        get() = false
    val serverEventLogs: List<String>
        get() = emptyList()
    val selectedDirectory: PlatformFile?
        get() = null
    val hostUrl: String?
        get() = null
    val isClientConfigured: Boolean
        get() = false
    val discoveredServers: List<DiscoveredServer>
        get() = emptyList()
    val isSearching: Boolean
        get() = false
    val configuredServerUrl: String?
        get() = null
    val serverName: String
        get() = ""
    val defaultServerName: String
        get() = ""
}