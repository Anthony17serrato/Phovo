package com.serratocreations.phovo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.serialization.SavedStateConfiguration
import com.serratocreations.phovo.core.navigation.NavigationState
import com.serratocreations.phovo.core.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.core.navigation.rememberNavigationState
import com.serratocreations.phovo.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun rememberPhovoAppState(
    savedStateConfig: SavedStateConfiguration
): PhovoAppState {
    val navigationState = rememberNavigationState(
        startRoute = PhotosHomeNavKey,
        topLevelRoutes = TOP_LEVEL_NAV_ITEMS.keys,
        savedStateConfig = savedStateConfig
    )

    //NavigationTrackingSideEffect(navigationState)

    return remember(
        navigationState
    ) {
        PhovoAppState(
            navigationState = navigationState
        )
    }
}

@Stable
class PhovoAppState(
    val navigationState: NavigationState,
) {
    lateinit var appLevelVmStoreOwner: ViewModelStoreOwner
}

///**
// * Stores information about navigation events to be used with JankStats
// */
//@Composable
//private fun NavigationTrackingSideEffect(navigationState: NavigationState) {
//    TrackDisposableJank(navigationState.currentKey) { metricsHolder ->
//        metricsHolder.state?.putState("Navigation", navigationState.currentKey.toString())
//        onDispose {}
//    }
//}