package com.serratocreations.phovo.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.ui.SearchRoute
import kotlinx.serialization.Serializable

// TODO Move to a dedicated search feature module
@Serializable object SearchRouteComponent: NavKey

fun NavController.navigateToBookmarks(navOptions: NavOptions) =
    navigate(route = SearchRouteComponent, navOptions)

fun NavGraphBuilder.bookmarksScreen() {
    composable<SearchRouteComponent> {
        SearchRoute()
    }
}