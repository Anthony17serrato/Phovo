package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.getPlatform
import com.serratocreations.phovo.core.navigation.Navigator
import com.serratocreations.phovo.core.navigation.PhotoDetailNavKey
import com.serratocreations.phovo.core.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.core.navigation.SharedViewModelStoreNavEntryDecorator
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.feature.photos.ui.PhotoViewerScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosHomeScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.util.handleVideoDesktop
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.photosEntries(
    sharedElementTransition: SharedTransitionScope,
    navigator: Navigator
) {
    entry<PhotosHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() }
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        PhotosHomeScreen(
            onPhotoClick = { uriPhotoUiItem ->
                photosViewModel.onPhotoClick(uriPhotoUiItem)
                if (uriPhotoUiItem is VideoPhotoUiItem && getPlatform() == Platform.Desktop) {
                    // Special video handling on desktop for now
                    // TODO[Low priority] Investigate if video can be displayed directly in app for desktop
                    handleVideoDesktop(uriPhotoUiItem.uri)
                } else {
                    navigator.navigate(PhotoDetailNavKey)
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
        PhotoViewerScreen(
            onBackClick = navigator::goBack,
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = LocalNavAnimatedContentScope.current,
            photosViewModel = photosViewModel
        )
    }
}