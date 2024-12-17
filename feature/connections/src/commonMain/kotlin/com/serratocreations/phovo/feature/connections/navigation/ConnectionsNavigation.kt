package com.serratocreations.phovo.feature.connections.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.serratocreations.phovo.feature.connections.ui.ConnectionsRoute
import kotlinx.serialization.Serializable

@Serializable object ConnectionsRoute

fun NavController.navigateToConnections(navOptions: NavOptions) =
    navigate(route = ConnectionsRoute, navOptions)

fun NavGraphBuilder.connectionsScreen() {
    composable<ConnectionsRoute> {
        ConnectionsRoute()
    }
}