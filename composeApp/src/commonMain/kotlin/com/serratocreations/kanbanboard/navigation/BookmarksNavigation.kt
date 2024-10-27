package com.serratocreations.kanbanboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.serratocreations.kanbanboard.ui.BookmarksRoute
import kotlinx.serialization.Serializable

@Serializable object BookmarksRoute

fun NavController.navigateToBookmarks(navOptions: NavOptions) =
    navigate(route = BookmarksRoute, navOptions)

fun NavGraphBuilder.bookmarksScreen() {
    composable<BookmarksRoute> {
        BookmarksRoute()
    }
}