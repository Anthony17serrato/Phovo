package com.serratocreations.phovo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModelStoreOwner
import com.serratocreations.phovo.core.navigation.NavigationState
import com.serratocreations.phovo.core.navigation.rememberNavigationState
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberPhovoAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): PhovoAppState {
    val navigationState = rememberNavigationState(ForYouNavKey, TOP_LEVEL_NAV_ITEMS.keys)

    //NavigationTrackingSideEffect(navigationState)

    return remember(
        navigationState,
        coroutineScope
    ) {
        PhovoAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope
        )
    }
}

@Stable
class PhovoAppState(
    val navigationState: NavigationState,
    coroutineScope: CoroutineScope,
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