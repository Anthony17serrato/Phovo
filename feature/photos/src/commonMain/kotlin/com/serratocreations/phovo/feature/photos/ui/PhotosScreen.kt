package com.serratocreations.phovo.feature.photos.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.serratocreations.phovo.core.designsystem.component.CallToActionComponent
import com.serratocreations.phovo.feature.photos.ui.model.BackupComplete
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgress
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatus
import com.serratocreations.phovo.feature.photos.ui.model.DateHeaderPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackup
import com.serratocreations.phovo.feature.photos.ui.model.UriPhotoUiItem
import com.serratocreations.phovo.feature.photos.ui.model.VideoPhotoUiItem
import com.serratocreations.phovo.feature.photos.util.getPlatformDecoderFactory
import com.serratocreations.phovo.feature.photos.util.getPlatformFetcherFactory
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PhotosRoute(
    onPhotoClick: (UriPhotoUiItem) -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    photosViewModel: PhotosViewModel,
    modifier: Modifier = Modifier,
) {
    // TODO move to root composable https://coil-kt.github.io/coil/image_loaders/
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(getPlatformDecoderFactory())
                add(getPlatformFetcherFactory())
            }
            .build()
    }
    val photosState by photosViewModel.photosUiState.collectAsStateWithLifecycle()
    PhotosScreen(
        photosItems = photosState.photosFeed,
        onPhotoClick = onPhotoClick,
        sharedElementTransition =sharedElementTransition,
        animatedContentScope = animatedContentScope,
        modifier = modifier
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun PhotosScreen(
    photosItems: List<PhotoUiItem>,
    onPhotoClick: (UriPhotoUiItem) -> Unit,
    sharedElementTransition: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    width: WindowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
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
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                CallToActionComponent(
                    actionTitle = "Finish setup",
                    actionDescription = "Get more from your gallery",
                    onClick = { /* TODO */ }
                )
            }
            itemsIndexed(
                items = photosItems,
                span = { index, item ->
                    when (item) {
                        is DateHeaderPhotoUiItem -> {
                            GridItemSpan(maxLineSpan)
                        }
                        is UriPhotoUiItem -> {
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
                            modifier = modifier.padding(16.dp)
                        )
                    }
                    is UriPhotoUiItem -> with(sharedElementTransition) {
                        val id = item.uri.toString()
                        Box {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = modifier
                                    .aspectRatio(1f)
                                    .sharedElement(
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
                                    modifier = modifier.align(Alignment.TopEnd)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpandableBackupBanner(backupState: BackupStatus) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        val size = ButtonDefaults.ExtraSmallContainerHeight
        AnimatedVisibility(isExpanded.not()) {
            ToggleButton(
                checked = false,
                onCheckedChange = { isExpanded = !isExpanded },
                modifier = Modifier.heightIn(size),
                contentPadding = ButtonDefaults.contentPaddingFor(size),
            ) {
                val thinStrokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
                val thinStroke =
                    remember(thinStrokeWidth) {
                        Stroke(
                            width = thinStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                when (backupState) {
                    is BackupComplete -> {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = stringResource(backupState.chipText),
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size))
                        )
                    }
                    is BackupInProgress -> {
                        val animatedProgress by
                        animateFloatAsState(
                            targetValue = backupState.progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        )
                        CircularWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
                            stroke = thinStroke,
                            trackStroke = thinStroke
                        )
                    }
                    PreparingBackup -> {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
                            stroke = thinStroke,
                            trackStroke = thinStroke
                        )
                    }
                }
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                Text(stringResource(backupState.chipText))
            }
        }
        AnimatedVisibility(isExpanded) {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = isExpanded,
                    onCheckedChange = { isExpanded = it },
                    modifier =
                        Modifier.heightIn(size).semantics {
                            stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                    contentPadding = ButtonDefaults.contentPaddingFor(size)
                ) {
                    val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        label = "Trailing Icon Rotation",
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(ButtonDefaults.iconSizeFor(size)).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        }

        // Animated Content
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (expandVertically(animationSpec = tween(300)) + fadeIn())
                    .togetherWith(
                        shrinkVertically(animationSpec = tween(300)) + fadeOut()
                    )
            },
            label = "BackupDetailsAnimation"
        ) { targetExpanded ->
            if (targetExpanded) {
                BackupSummaryCard(
                    status = backupState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupSummaryCard(
    status: BackupStatus
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = status.header.build(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // ✅ Top Row: Backed up photos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    PreparingBackup -> {
                        CircularWavyProgressIndicator()
                    }
                    is BackupInProgress -> {
                        val animatedProgress by
                        animateFloatAsState(
                            targetValue = status.progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        )
                        CircularWavyProgressIndicator(
                            progress = { animatedProgress }
                        )
                    }
                    is BackupComplete -> {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = status.mainStatus.build(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            AnimatedVisibility(visible = status.statusDescription != null) {
                HorizontalDivider(
                    modifier = Modifier,
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(16.dp)
                ) {
                    status.statusDescription?.let {
                        Text(
                            text = it.build(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    status.actionButton?.let {
                        OutlinedButton(
                            onClick = it.onActionButtonClick,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(it.actionText),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}