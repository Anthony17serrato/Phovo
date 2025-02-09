package com.serratocreations.phovo.core.common.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PhovoViewModel: ViewModel() {
    private val _phovoUiState = MutableStateFlow(PhovoUiState())
    val phovoUiState = _phovoUiState.asStateFlow()

    /**
     * Updates UI state to show back button in cases where it is required such as on smaller devices
     */
    fun showBackButtonIfRequired(show: Boolean) {
        _phovoUiState.value = _phovoUiState.value.copy(
            canBackButtonBeShown = show
        )
    }

    fun onNavigationClick() {
        _phovoUiState.update { uiState ->
            uiState.copy(
                navigationUpClicked = true
            )
        }
    }

    fun onNavigationUpClickHandled() {
        _phovoUiState.update { uiState ->
            uiState.copy(
                navigationUpClicked = false
            )
        }
    }
}

data class PhovoUiState(
    val canBackButtonBeShown: Boolean = false,
    val navigationUpClicked: Boolean = false,
)