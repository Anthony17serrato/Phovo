package com.serratocreations.kanbanboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.serratocreations.kanbanboard.ui.InterestsRoute
import kotlinx.serialization.Serializable

@Serializable object InterestsRoute

fun NavController.navigateToInterests(navOptions: NavOptions) =
    navigate(route = InterestsRoute, navOptions)

fun NavGraphBuilder.interestsScreen() {
    composable<InterestsRoute> {
        InterestsRoute()
    }
}