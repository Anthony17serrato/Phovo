package com.serratocreations.phovo.feature.photos.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.serratocreations.phovo.feature.photos.ui.PhotoDetailRoute
import com.serratocreations.phovo.feature.photos.ui.PhotosRoute
import kotlinx.serialization.Serializable

@Serializable
object PhotosRoute

@Serializable
data class PhotoDetailRoute(val uri: String)

fun NavController.navigateToForYou(navOptions: NavOptions) =
    navigate(route = PhotosRoute, navOptions)

fun NavController.navigateToPhotoDetail(uri: String) =
    navigate(route = PhotoDetailRoute(uri))

fun NavGraphBuilder.photosScreen(
    onNavigateToPhotoDetail: (String) -> Unit
) {
    composable<PhotosRoute> {
        PhotosRoute(
            onPhotoClick = onNavigateToPhotoDetail
        )
    }
}

fun NavGraphBuilder.photoDetailScreen(
    onBackClick: () -> Unit
) {
    composable<PhotoDetailRoute> { backStackEntry ->
        val uri = backStackEntry.arguments?.getString("uri") ?: ""
        PhotoDetailRoute(
            uri = uri,
            onBackClick = onBackClick
        )
    }
}
