package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable
import com.serratocreations.phovo.core.navigation.ExpandableHeaderState

/**
 * @param headerState opens and closes [PhotosHomeHeaderContent], for platforms whose title shows
 *   backup status.
 */
@Composable
expect fun PhotosHomeTitleContent(headerState: ExpandableHeaderState)

/**
 * Shown under the top app bar while [PhotosHomeTitleContent] has it open. Empty where the title
 * has nothing to expand.
 *
 * @param onFinishServerSetup take the user to where a server is set up.
 */
@Composable
expect fun PhotosHomeHeaderContent(onFinishServerSetup: () -> Unit)
