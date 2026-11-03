package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import org.jetbrains.compose.resources.painterResource
import phovo.feature.photos.generated.resources.*
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
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.designsystem.constants.CommonDimensions.defaultIconSize
import com.serratocreations.phovo.core.navigation.ExpandableHeaderState
import com.serratocreations.phovo.feature.photos.ui.AppStateViewModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatusAction
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatusIndicator
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatusUiModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The backup chip in the top app bar's row. It opens [BackupSummaryHeader] below the bar, and
 * turns into the button that closes it again while that is open.
 */
@OptIn( ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupStatusChip(
    headerState: ExpandableHeaderState,
    appStateViewModel: AppStateViewModel = koinViewModel()
) {
    val backupState by appStateViewModel.backupUiState.collectAsState()
    val isExpanded = headerState.isExpanded
    val size = ButtonDefaults.ExtraSmallContainerHeight
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        label = "BackupChipAnimation"
    ) { targetExpanded ->
        if (!targetExpanded) {
            ToggleButton(
                checked = false,
                onCheckedChange = { headerState.expand() },
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
                BackupStatusIndicatorView(
                    indicator = backupState.indicator,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
                    stroke = thinStroke,
                    trackStroke = thinStroke
                )
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                Text(stringResource(backupState.chipText))
            }
        } else {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = true,
                    onCheckedChange = { headerState.collapse() },
                    modifier =
                        Modifier.heightIn(size).semantics {
                            stateDescription = "Expanded"
                            contentDescription = description
                        },
                    contentPadding = ButtonDefaults.contentPaddingFor(size)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_keyboard_arrow_down_default),
                        modifier =
                            Modifier.size(ButtonDefaults.iconSizeFor(size)).graphicsLayer {
                                this.rotationZ = 180f
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        }
    }
}

/** The backup summary [BackupStatusChip] opens below the top app bar. */
@Composable
fun BackupSummaryHeader(
    onFinishServerSetup: () -> Unit,
    appStateViewModel: AppStateViewModel = koinViewModel()
) {
    val backupState by appStateViewModel.backupUiState.collectAsState()
    // Exhaustive on purpose, so a new action has to be routed here rather than silently doing
    // nothing.
    val onActionClick: (BackupStatusAction) -> Unit = { action ->
        when (action) {
            BackupStatusAction.AllowPhotoAccess -> appStateViewModel.onAllowPhotoAccess()
            BackupStatusAction.SetUpServer -> onFinishServerSetup()
            BackupStatusAction.ViewFailedItems -> appStateViewModel.onViewFailedItems()
        }
    }
    BackupSummaryCard(
        status = backupState,
        onActionClick = onActionClick
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupSummaryCard(
    status: BackupStatusUiModel,
    onActionClick: (BackupStatusAction) -> Unit
) {
    Column(
        modifier = Modifier
            .animateContentSize()
            .padding(bottom = SummaryBottomPadding)
            .fillMaxWidth()
    ) {
        Text(
            text = status.header.build(),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackupStatusIndicatorView(
                    indicator = status.indicator,
                    iconModifier = Modifier.size(defaultIconSize)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = status.mainStatus.build(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // The action button lives in this section too, so it shows for either.
            AnimatedVisibility(
                visible = status.statusDescription != null || status.actionButton != null
            ) {
                HorizontalDivider(
                    modifier = Modifier,
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            onClick = { onActionClick(it.action) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .heightIn(min = 48.dp)
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

/** Keeps the feed clear of the summary, and wide enough for its rounded corners to show. */
private val SummaryBottomPadding = 28.dp

/**
 * Draws [indicator]. It always sits next to text that says the same thing, so icons carry no
 * content description of their own.
 *
 * @param modifier applied to whichever indicator is drawn
 * @param iconModifier applied to icons instead of [modifier], for callers that size icons apart
 *   from progress indicators
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupStatusIndicatorView(
    indicator: BackupStatusIndicator,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = modifier,
    stroke: Stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.circularTrackStroke
) {
    when (indicator) {
        BackupStatusIndicator.Indeterminate -> {
            CircularWavyProgressIndicator(
                modifier = modifier,
                stroke = stroke,
                trackStroke = trackStroke
            )
        }
        is BackupStatusIndicator.Progress -> {
            val animatedProgress by
            animateFloatAsState(
                targetValue = indicator.progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            )
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = modifier,
                stroke = stroke,
                trackStroke = trackStroke,
                // The default flattens the wave below 10% and above 95%, so a large library
                // starting out looks stalled. Keep it moving for as long as work is in progress.
                amplitude = { 1f }
            )
        }
        is BackupStatusIndicator.Icon -> {
            Icon(
                painter = painterResource(indicator.icon),
                contentDescription = null,
                modifier = iconModifier
            )
        }
    }
}
