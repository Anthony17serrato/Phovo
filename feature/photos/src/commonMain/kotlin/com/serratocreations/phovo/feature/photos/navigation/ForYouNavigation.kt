package com.serratocreations.phovo.feature.photos.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.serratocreations.phovo.feature.photos.ui.PhotosRoute
import kotlinx.serialization.Serializable

@Serializable
object PhotosRoute

fun NavController.navigateToForYou(navOptions: NavOptions) =
    navigate(route = PhotosRoute, navOptions)

fun NavGraphBuilder.photosScreen() {
    composable<PhotosRoute> {
        PhotosRoute()
    }
}