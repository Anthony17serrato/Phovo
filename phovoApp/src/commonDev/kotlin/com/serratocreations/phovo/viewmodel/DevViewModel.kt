package com.serratocreations.phovo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.DevLogicManager
import com.serratocreations.phovo.navigation.DevMenuResetOptionsNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevViewModel(
    private val devLogicManager: DevLogicManager,
): ViewModel() {
    private val _uiState = MutableStateFlow(DevUiState())
    val uiState: StateFlow<DevUiState> = _uiState.asStateFlow()

    fun onClickResetAppState() = viewModelScope.launch {
        devLogicManager.resetAppState()

    }
}

data class DevUiState(
    val devMenuListOfRoutes: List<NavKey> = listOf(
        DevMenuResetOptionsNavKey
    )
)