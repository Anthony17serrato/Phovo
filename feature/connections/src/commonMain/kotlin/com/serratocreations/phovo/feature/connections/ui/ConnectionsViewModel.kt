package com.serratocreations.phovo.feature.connections.ui

import androidx.lifecycle.ViewModel
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectionsViewModel(
    private val serverConfigRepository: ServerConfigRepository
): ViewModel() {
    protected abstract val initialState: ConnectionsUiState

    abstract val connectionsUiState: StateFlow<ConnectionsUiState>
}

/**
 * Connections UI data model which contains properties that
 * are common for both client & server UI State.
 */
interface ConnectionsUiState