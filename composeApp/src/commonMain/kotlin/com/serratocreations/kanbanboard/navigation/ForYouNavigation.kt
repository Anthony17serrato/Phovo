package com.serratocreations.kanbanboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.serratocreations.kanbanboard.ui.ForYouRoute
import kotlinx.serialization.Serializable

@Serializable object ForYouRoute

fun NavController.navigateToForYou(navOptions: NavOptions) =
    navigate(route = ForYouRoute, navOptions)

fun NavGraphBuilder.forYouScreen() {
    composable<ForYouRoute> {
        ForYouRoute()
    }
}