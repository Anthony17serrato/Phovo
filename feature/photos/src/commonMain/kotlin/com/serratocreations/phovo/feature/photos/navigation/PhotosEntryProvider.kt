package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
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
    onShowAppBarRequested: () -> Unit,
    scaffoldPadding: PaddingValues
) {
    entry<PhotosHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() }
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if(navigationViewModel.state.currentKey == PhotosHomeNavKey) {
                navigationViewModel.setAppBarConfig(
                    AppBarConfig(
                        title = { PhotosHomeTitleContent() },
                        topAppBarColors = {
                            val defaultColors = TopAppBarDefaults.topAppBarColors()
                            defaultColors.copy(
                                containerColor = defaultColors.containerColor.copy(alpha = 0f),
                                scrolledContainerColor = defaultColors.containerColor.copy(alpha = 0f)
                            )
                        }
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
            photosViewModel = photosViewModel,
            modifier = Modifier.padding(scaffoldPadding)
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
                        },
                        topAppBarColors = {
                            val defaultColors = TopAppBarDefaults.topAppBarColors()
                            defaultColors.copy(
                                containerColor = defaultColors.containerColor.copy(alpha = 0.7f),
                                scrolledContainerColor = defaultColors.containerColor.copy(alpha = 0.8f)
                            )
                        }
                    )
                )
            }
        }
        val layoutDirection = LocalLayoutDirection.current
        val viewerModifier = Modifier.padding(
            top = 0.dp,
            bottom = scaffoldPadding.calculateBottomPadding(),
            start = scaffoldPadding.calculateStartPadding(layoutDirection),
            end = scaffoldPadding.calculateEndPadding(layoutDirection)
        )
        PhotoViewerScreen(
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = LocalNavAnimatedContentScope.current,
            photosViewModel = photosViewModel,
            modifier = viewerModifier
        )
    }
}