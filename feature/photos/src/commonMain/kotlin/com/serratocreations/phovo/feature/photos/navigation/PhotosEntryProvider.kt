package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.getPlatform
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.core.navigation.PhotoDetailNavKey
import com.serratocreations.phovo.core.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.core.navigation.SharedViewModelStoreNavEntryDecorator
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.feature.photos.ui.PhotoViewerScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosHomeScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import com.serratocreations.phovo.feature.photos.ui.components.PhotosHomeTitleContent
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.util.handleVideoDesktop
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.photosEntries(
    sharedElementTransition: SharedTransitionScope,
    navigationViewModel: NavigationViewModel
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
                photosViewModel.onPhotoClick(uriPhotoUiItem)
                if (uriPhotoUiItem is VideoPhotoUiItem && getPlatform() == Platform.Desktop) {
                    // Special video handling on desktop for now
                    // TODO[Low priority] Investigate if video can be displayed directly in app for desktop
                    handleVideoDesktop(uriPhotoUiItem.uri)
                } else {
                    navigationViewModel.navigate(PhotoDetailNavKey)
                }
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
                        title = { Text("Test") },
                        navigationIcon = {
                            IconButton(onClick = navigationViewModel::goBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    // TODO Extract string resource
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
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