package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.photosScreen(
    sharedElementTransition: SharedTransitionScope,
    onNavigateToPhotoDetail: (String) -> Unit
) {
    composable<PhotosRoute> {
        PhotosRoute(
            onPhotoClick = onNavigateToPhotoDetail,
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = this@composable,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.photoDetailScreen(
    sharedElementTransition: SharedTransitionScope,
    onBackClick: () -> Unit
) {
    composable<PhotoDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PhotoDetailRoute>()
        PhotoDetailRoute(
            uri = route.uri,
            onBackClick = onBackClick,
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = this@composable,
        )
    }
}
