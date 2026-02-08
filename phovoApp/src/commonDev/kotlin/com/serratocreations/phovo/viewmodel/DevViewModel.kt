package com.serratocreations.phovo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.navigation.DevMenuResetOptionsNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DevViewModel(

): ViewModel() {
    private val _uiState = MutableStateFlow(DevUiState())
    val uiState: StateFlow<DevUiState> = _uiState.asStateFlow()

}

data class DevUiState(
    val devMenuListOfRoutes: List<NavKey> = listOf(
        DevMenuResetOptionsNavKey
    )
)