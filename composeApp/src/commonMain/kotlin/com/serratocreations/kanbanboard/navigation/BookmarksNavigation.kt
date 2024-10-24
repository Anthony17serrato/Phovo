package com.serratocreations.kanbanboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable object BookmarksRoute

fun NavController.navigateToBookmarks(navOptions: NavOptions) =
    navigate(route = BookmarksRoute, navOptions)

fun NavGraphBuilder.bookmarksScreen(
    onTopicClick: (String) -> Unit,
    onShowSnackbar: suspend (String, String?) -> Boolean,
) {
    composable<BookmarksRoute> {
        BookmarksRoute(onTopicClick, onShowSnackbar)
    }
}