package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.runtime.Composable

/**
 * @param onFinishServerSetup take the user to where a server is set up, for platforms whose title
 *   shows backup status.
 */
@Composable
expect fun PhotosHomeTitleContent(onFinishServerSetup: () -> Unit)