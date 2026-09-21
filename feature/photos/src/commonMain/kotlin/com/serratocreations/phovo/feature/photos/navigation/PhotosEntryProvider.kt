package com.serratocreations.phovo.feature.photos.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.DefaultNavigationIcon
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.core.navigation.SharedViewModelStoreNavEntryDecorator
import com.serratocreations.phovo.core.navigation.toContentKey
import com.serratocreations.phovo.feature.photos.ui.CallToActionAction
import com.serratocreations.phovo.feature.photos.ui.CallToActionsScreen
import com.serratocreations.phovo.feature.photos.ui.PhotoViewerScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosHomeScreen
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import com.serratocreations.phovo.feature.photos.ui.components.PhotosHomeTitleContent
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
/**
 * @param onFinishServerSetup take the user to where a server is set up. Photos cannot name that
 *   destination - it lives in another feature - so the app supplies it.
 */
fun EntryProviderScope<NavKey>.photosEntries(
    sharedElementTransition: SharedTransitionScope,
    navigationViewModel: NavigationViewModel,
    onShowAppBarRequested: () -> Unit,
    onFinishServerSetup: () -> Unit,
    scaffoldPadding: PaddingValues
) {
    entry<PhotosHomeNavKey>(
        clazzContentKey = { key -> key.toContentKey() }
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        val appBarConfig: AppBarConfig = remember {
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
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if(navigationViewModel.state.currentKey == PhotosHomeNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }

        PhotosHomeScreen(
            onPhotoClick = { uriPhotoUiItem ->
                onShowAppBarRequested()
                photosViewModel.onPhotoSelected(uriPhotoUiItem)
                navigationViewModel.navigate(PhotoDetailNavKey)
            },
            onSeeAllCallToActions = {
                onShowAppBarRequested()
                navigationViewModel.navigate(CallToActionsNavKey)
            },
            onCallToActionClick = { action ->
                onCallToActionClicked(action, photosViewModel, onFinishServerSetup)
            },
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = LocalNavAnimatedContentScope.current,
            photosViewModel = photosViewModel,
            isCurrentDestination = navigationViewModel.state.currentKey == PhotosHomeNavKey,
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
    entry<PhotoDetailNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = PhotosHomeNavKey.toContentKey()
        )
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        var areBarsVisible by remember { mutableStateOf(true) }
        val appBarConfig = remember(areBarsVisible) {
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
                },
                shouldOverlayTopAppBar = true,
                showBottomAppBar = false,
                showBottomToolbar = areBarsVisible,
                showTopAppBar = areBarsVisible
            )
        }
        LaunchedEffect(appBarConfig) {
            if(navigationViewModel.state.currentKey == PhotoDetailNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }
        PhotoViewerScreen(
            sharedElementTransition = sharedElementTransition,
            animatedContentScope = LocalNavAnimatedContentScope.current,
            photosViewModel = photosViewModel,
            areBarsVisible = areBarsVisible,
            onToggleBars = { areBarsVisible = !areBarsVisible },
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
    entry<CallToActionsNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            contentKey = PhotosHomeNavKey.toContentKey()
        )
    ) {
        val photosViewModel: PhotosViewModel = koinViewModel()
        val appBarConfig: AppBarConfig = remember {
            AppBarConfig(
                title = { Text("Needs attention") },
                navigationIcon = {
                    DefaultNavigationIcon(navigationViewModel::goBack)
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == CallToActionsNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }
        CallToActionsScreen(
            photosViewModel = photosViewModel,
            onCallToActionClick = { action ->
                onCallToActionClicked(action, photosViewModel, onFinishServerSetup)
            },
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
}

/**
 * Split a call to action between the two layers that can act on it: the view model owns anything
 * that touches platform state, the UI owns anything that moves the user. The `when` is exhaustive
 * on purpose, so a new action has to be routed here rather than silently doing nothing.
 */
private fun onCallToActionClicked(
    action: CallToActionAction,
    photosViewModel: PhotosViewModel,
    onFinishServerSetup: () -> Unit
) {
    when (action) {
        CallToActionAction.FinishServerSetup -> onFinishServerSetup()
        CallToActionAction.RequestGalleryPermission,
        CallToActionAction.OpenPermissionSettings -> photosViewModel.onCallToActionClicked(action)
    }
}
