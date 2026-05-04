package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.DefaultNavigationIcon
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.core.navigation.SharedViewModelStoreNavEntryDecorator
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.feature.photos.ui.PhotoViewerScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosHomeScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import com.serratocreations.phovo.feature.photos.ui.components.PhotosHomeTitleContent
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.photosEntries(
    sharedElementTransition: SharedTransitionScope,
    navigationViewModel: NavigationViewModel,
    onShowAppBarRequested: () -> Unit
) {
    entry<PhotosHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() }
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if(navigationViewModel.state.currentKey == PhotosHomeNavKey) {
                navigationViewModel.setAppBarConfig(
                    AppBarConfig(
                        title = { PhotosHomeTitleContent() }
                    )
                )
            }
        }
        PhotosHomeScreen(
            onPhotoClick = { uriPhotoUiItem ->
                onShowAppBarRequested()
                photosViewModel.onPhotoClick(uriPhotoUiItem)
                navigationViewModel.navigate(PhotoDetailNavKey)
            },
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = LocalNavAnimatedContentScope.current,
            photosViewModel = photosViewModel
        )
    }
    entry<PhotoDetailNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = PhotosHomeNavKey.toContentKey()
        )
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if(navigationViewModel.state.currentKey == PhotoDetailNavKey) {
                navigationViewModel.setAppBarConfig(
                    AppBarConfig(
                        // TODO Display photo date instead
                        title = { Text("Details") },
                        navigationIcon = {
                            DefaultNavigationIcon(navigationViewModel::goBack)
                        }
                    )
                )
            }
        }
        PhotoViewerScreen(
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = LocalNavAnimatedContentScope.current,
            photosViewModel = photosViewModel
        )
    }
}