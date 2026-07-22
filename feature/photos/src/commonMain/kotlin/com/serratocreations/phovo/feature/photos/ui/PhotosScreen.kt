package com.serratocreations.phovo.feature.photos.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.serratocreations.phovo.core.common.Platform
import com.serratocreations.phovo.core.common.getPlatform
import com.serratocreations.phovo.core.common.PermissionState
import com.serratocreations.phovo.core.common.rememberPermissionRequester
import com.serratocreations.phovo.core.common.getActivity
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.feature.photos.ui.components.LoadMultiResImage
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.MediaUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.util.LocalOrRemoteAssetMapper
import com.serratocreations.phovo.feature.photos.util.getPlatformDecoderFactory
import com.serratocreations.phovo.feature.photos.util.getPlatformFetcherFactory
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import okio.FileSystem
import okio.Path.Companion.toPath

fun ImageLoader.Builder.platformDiskCache(): ImageLoader.Builder =
    this.diskCache {
        // TODO Temporary fix ios file .absolutePath issue
        if (getPlatform() == Platform.Ios) {
            DiskCache.Builder().directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(512L * 1024 * 1024) // 512MB
                .build()
        } else {
            val directory = (FileKit.cacheDir / "image_cache")
            directory.createDirectories(mustCreate = false)
            DiskCache.Builder()
                .directory(directory.absolutePath().toPath())
                .build()
        }
    }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PhotosHomeScreen(
    onPhotoClick: (MediaUiItem) -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    photosViewModel: PhotosViewModel,
    modifier: Modifier = Modifier
) {
    // TODO move to root composable https://coil-kt.github.io/coil/image_loaders/
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.30) // 30% of app memory
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .platformDiskCache()
            .logger(DebugLogger())
            .components {
                add(getPlatformDecoderFactory())
                add(getPlatformFetcherFactory())
                add(LocalOrRemoteAssetMapper())
                addPlatformFileSupport()
            }
            .build()
    }

    val activity = getActivity()
    val photosState by photosViewModel.photosUiState.collectAsStateWithLifecycle()

    // Lifecycle observer to refresh permission status when returning from settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                photosViewModel.checkPermissionState(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Refresh permission state on launch/activity bind
    LaunchedEffect(activity) {
        photosViewModel.checkPermissionState(activity)
    }

    var dismissedBottomSheet by remember { mutableStateOf(false) }
    val permissionRequester = rememberPermissionRequester { isGranted ->
        photosViewModel.checkPermissionState(activity)
    }

    if (photosState.permissionState == PermissionState.NotDetermined && !dismissedBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { dismissedBottomSheet = true }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Key Visual M3 Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhovoIcons.PhovoIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Welcome to Phovo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = "To automatically back up your photos and show them in your feed, Phovo needs access to your gallery. Without this, Phovo operates as a dashboard only for images from your server.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { dismissedBottomSheet = true }
                    ) {
                        Text("Maybe later")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            permissionRequester.requestPermission()
                        }
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }

    val showDeniedCard = photosState.permissionState == PermissionState.Denied ||
            (photosState.permissionState == PermissionState.NotDetermined && dismissedBottomSheet)

    PhotosScreen(
        photosItems = photosState.photosFeed,
        permissionState = photosState.permissionState,
        showDeniedCard = showDeniedCard,
        onCardClick = {
            if (photosState.permissionState == PermissionState.NotDetermined) {
                permissionRequester.requestPermission()
            } else {
                photosViewModel.openSettings()
            }
        },
        onPhotoClick = onPhotoClick,
        sharedElementTransition = sharedElementTransition,
        animatedContentScope = animatedContentScope,
        modifier = modifier
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun PhotosScreen(
    photosItems: List<PhotoUiItem>,
    permissionState: PermissionState,
    showDeniedCard: Boolean = false,
    onCardClick: () -> Unit,
    onPhotoClick: (MediaUiItem) -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    // TODO Refactor based on already implemented code in Navigation.kt
    width: WindowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(
                minSize = when (width) {
                    WindowWidthSizeClass.COMPACT -> 80.dp
                    WindowWidthSizeClass.MEDIUM -> 120.dp
                    WindowWidthSizeClass.EXPANDED -> 160.dp
                    else -> 80.dp
                }
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showDeniedCard) {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    CallToActionComponent(
                        actionTitle = "Backups are disabled",
                        actionDescription = "Your device images are not backed up. Phovo is operating as a server-dashboard only. Tap to open Settings and allow access.",
                        onClick = onCardClick
                    )
                }
            } else if (permissionState == PermissionState.Limited) {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    CallToActionComponent(
                        actionTitle = "Limited library access",
                        actionDescription = "Phovo needs access to all of your photos and videos to back them up automatically. Tap to open Settings and allow full access.",
                        onClick = onCardClick
                    )
                }
            }

            itemsIndexed(
                items = photosItems,
                key = { _, item ->
                    item.key
                },
                span = { index, item ->
                    when (item) {
                        is DateHeaderPhotoUiItem -> {
                            GridItemSpan(maxLineSpan)
                        }
                        is MediaUiItem -> {
                            GridItemSpan(1)
                        }
                    }
                }
            ) { index, item ->
                when (item) {
                    is DateHeaderPhotoUiItem -> {
                        Text(
                            // TODO: month must come from a localized string, consider an enum class with
                            //  string res values
                            text = "${item.month} ${item.year ?: ""}",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    is MediaUiItem -> with(sharedElementTransition) {
                        val id = item.key
                        Box(modifier = Modifier.aspectRatio(1f)) {
                            LoadMultiResImage(
                                highRes = item.thumbnail,
                                lowRes = item.lowResThumbnail,
                                contentScale = ContentScale.Crop,
                                shouldLoadSequentially = true,
                                modifier = Modifier.sharedBounds(
                                    sharedContentState = sharedElementTransition
                                        .rememberSharedContentState(key = "image-$id"),
                                    animatedVisibilityScope = animatedContentScope
                                ).clickable { onPhotoClick(item) }
                            )
                            if (item is VideoPhotoUiItem) {
                                Text(
                                    text = item.duration,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}