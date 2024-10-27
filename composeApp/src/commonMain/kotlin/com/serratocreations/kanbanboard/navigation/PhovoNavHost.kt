package com.serratocreations.kanbanboard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.serratocreations.kanbanboard.ui.PhovoAppState

/**
 * Top-level navigation graph. Navigation is organized as explained at
 * https://d.android.com/jetpack/compose/nav-adaptive
 *
 * The navigation graph defined in this file defines the different top level routes. Navigation
 * within each route is handled using state and Back Handlers.
 */
@Composable
fun PhovoNavHost(
    appState: PhovoAppState,
    modifier: Modifier = Modifier,
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = ForYouRoute,
        modifier = modifier,
    ) {
        forYouScreen()
        bookmarksScreen()
        interestsScreen()
    }
}