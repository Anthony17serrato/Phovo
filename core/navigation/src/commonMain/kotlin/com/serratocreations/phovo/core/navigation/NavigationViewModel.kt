package com.serratocreations.phovo.core.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class NavigationViewModel(
    val state: NavigationState
): ViewModel() {
    private val _appBarState = MutableStateFlow(AppBarConfig())
    val appBarState = _appBarState.asStateFlow()

    fun navigate(route: NavKey){
        if (route in state.backStacks.keys){
            // This is a top level route, just switch to it
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack(){
        val currentStack = state.backStacks[state.topLevelRoute] ?:
        error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute){
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    fun setAppBarConfig(appBarConfig: AppBarConfig) {
        _appBarState.update {
            appBarConfig
        }
    }
}