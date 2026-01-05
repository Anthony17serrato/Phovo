package com.serratocreations.phovo.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.serratocreations.phovo.feature.connections.ui.connectionsDetailsScreen
import com.serratocreations.phovo.feature.photos.navigation.PhotosNavGraphRoute
import com.serratocreations.phovo.feature.photos.navigation.photosNavGraph
import com.serratocreations.phovo.ui.PhovoAppState
import kotlinx.serialization.Serializable

@Serializable data object PhovoBaseRoute

/**
 * Top-level navigation graph. Navigation is organized as explained at
 * https://d.android.com/jetpack/compose/nav-adaptive
 *
 * The navigation graph defined in this file defines the different top-level routes. Navigation
 * within each route is handled using state and Back Handlers.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhovoNavHost(
    appState: PhovoAppState,
    modifier: Modifier = Modifier,
) {
    val navController = appState.navController
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = PhovoBaseRoute,
            modifier = modifier,
        ) {
            navigation<PhovoBaseRoute>(startDestination = PhotosNavGraphRoute) {
                photosNavGraph(this@SharedTransitionLayout,navController)
                bookmarksScreen()
                connectionsDetailsScreen(appState.appLevelVmStoreOwner)
            }
        }
    }
}
