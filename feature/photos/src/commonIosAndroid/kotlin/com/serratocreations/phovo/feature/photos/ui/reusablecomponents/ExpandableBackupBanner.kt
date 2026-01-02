package com.serratocreations.phovo.feature.photos.ui.reusablecomponents

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.serratocreations.phovo.feature.photos.ui.BackupStatusViewModel
import com.serratocreations.phovo.feature.photos.ui.model.BackupComplete
import com.serratocreations.phovo.feature.photos.ui.model.BackupInProgress
import com.serratocreations.phovo.feature.photos.ui.model.BackupStatus
import com.serratocreations.phovo.feature.photos.ui.model.PreparingBackup
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn( ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableBackupBanner(
    backupStatusViewModel: BackupStatusViewModel = koinViewModel()
) {
    val backupState by backupStatusViewModel.backupUiState.collectAsState()
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
                when (val backupState = backupState) {
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
            .animateContentSize()
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