package com.serratocreations.phovo.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import com.serratocreations.phovo.core.designsystem.component.PhovoBackground

/**
 * Returns a [NavEntryDecorator] that wraps each [NavEntry] in a [PhovoBackground], making every
 * destination opaque and full-bleed.
 *
 * The iOS transitions that `NavDisplay` uses by default veil the outgoing entry (`veilOut`) inside
 * that entry's own bounds, the way UIKit dims the outgoing view controller during a push. That only
 * looks right if destinations are opaque and fill their container. Phovo's are neither: the app's
 * only background sits behind `NavDisplay`, and screens such as `SearchScreen` measure no taller
 * than their content. Without this decorator the veil shows up as a hard-edged grey rectangle sized
 * to whatever the outgoing screen happened to measure, and the strip the outgoing screen slides
 * away from is never covered.
 */
@Composable
fun <T : Any> rememberBackgroundNavEntryDecorator(): NavEntryDecorator<T> = remember {
    NavEntryDecorator { entry ->
        PhovoBackground {
            entry.Content()
        }
    }
}
