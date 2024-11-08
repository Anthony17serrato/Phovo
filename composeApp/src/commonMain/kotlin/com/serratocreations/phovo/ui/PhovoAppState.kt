package com.serratocreations.phovo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.serratocreations.phovo.feature.photos.navigation.navigateToForYou
import com.serratocreations.phovo.navigation.TopLevelDestination
import com.serratocreations.phovo.navigation.TopLevelDestination.FOR_YOU
import com.serratocreations.phovo.navigation.TopLevelDestination.INTERESTS
import com.serratocreations.phovo.navigation.TopLevelDestination.BOOKMARKS
import com.serratocreations.phovo.navigation.navigateToBookmarks
import com.serratocreations.phovo.navigation.navigateToInterests
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberPhovoAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): PhovoAppState {
    return remember(
        navController,
        coroutineScope
    ) {
        PhovoAppState(
            navController = navController,
            coroutineScope = coroutineScope
        )
    }
}

@Stable
class PhovoAppState(
    val navController: NavHostController,
    coroutineScope: CoroutineScope,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            return TopLevelDestination.entries.firstOrNull { topLevelDestination ->
                currentDestination?.hasRoute(route = topLevelDestination.route) ?: false
            }
        }

    /**
     * Map of top level destinations to be used in the TopBar, BottomBar and NavRail. The key is the
     * route.
     */
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    /**
     * UI logic for navigating to a top level destination in the app. Top level destinations have
     * only one copy of the destination of the back stack, and save and restore state whenever you
     * navigate to and from it.
     *
     * @param topLevelDestination: The destination the app needs to navigate to.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }

        when (topLevelDestination) {
            FOR_YOU -> navController.navigateToForYou(topLevelNavOptions)
            BOOKMARKS -> navController.navigateToBookmarks(topLevelNavOptions)
            INTERESTS -> navController.navigateToInterests(topLevelNavOptions)
        }
    }
}

///**
// * Stores information about navigation events to be used with JankStats
// */
//@Composable
//private fun NavigationTrackingSideEffect(navController: NavHostController) {
//    TrackDisposableJank(navController) { metricsHolder ->
//        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
//            metricsHolder.state?.putState("Navigation", destination.route.toString())
//        }
//
//        navController.addOnDestinationChangedListener(listener)
//
//        onDispose {
//            navController.removeOnDestinationChangedListener(listener)
//        }
//    }
//}