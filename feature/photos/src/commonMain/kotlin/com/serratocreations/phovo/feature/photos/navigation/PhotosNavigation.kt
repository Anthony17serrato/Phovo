package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.serratocreations.phovo.feature.photos.ui.PhotoDetailRoute
import com.serratocreations.phovo.feature.photos.ui.PhotosRoute
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object PhotosNavGraphRoute
@Serializable
object PhotosRoute

@Serializable
data object PhotoDetailRoute

fun NavController.navigateToForYou(navOptions: NavOptions) =
    navigate(route = PhotosRoute, navOptions)

fun NavController.navigateToPhotoDetail() =
    navigate(route = PhotoDetailRoute)

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.photosNavGraph(
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController
) {
    navigation<PhotosNavGraphRoute>(startDestination = PhotosRoute) {
        photosScreen(
            sharedElementTransition = sharedTransitionScope,
            onNavigateToPhotoDetail = {
                navController.navigateToPhotoDetail()
            },
            navController = navController
        )
        photoDetailScreen(
            sharedElementTransition = sharedTransitionScope,
            onBackClick = {
                navController.popBackStack()
            },
            navController = navController
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.photosScreen(
    sharedElementTransition: SharedTransitionScope,
    onNavigateToPhotoDetail: () -> Unit,
    navController: NavController
) {
    composable<PhotosRoute> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(PhotosNavGraphRoute)
        }
        // Get the ViewModel scoped to the `PhotoDetailRoute` Nav graph
        val photosViewModel: PhotosViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        PhotosRoute(
            onPhotoClick = { uriPhotoUiItem ->
                photosViewModel.onPhotoClick(uriPhotoUiItem)
                onNavigateToPhotoDetail()
            },
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = this@composable,
            photosViewModel = photosViewModel
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.photoDetailScreen(
    sharedElementTransition: SharedTransitionScope,
    onBackClick: () -> Unit,
    navController: NavController
) {
    composable<PhotoDetailRoute> { backStackEntry ->
        //val route = backStackEntry.toRoute<PhotoDetailRoute>()
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(PhotosNavGraphRoute)
        }
        // Get the ViewModel scoped to the `PhotoDetailRoute` Nav graph
        val photosViewModel: PhotosViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        PhotoDetailRoute(
            onBackClick = onBackClick,
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = this@composable,
            photosViewModel = photosViewModel
        )
    }
}
