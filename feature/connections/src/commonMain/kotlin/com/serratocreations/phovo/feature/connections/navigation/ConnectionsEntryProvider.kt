package com.serratocreations.phovo.feature.connections.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.NavigationViewModel

fun EntryProviderScope<NavKey>.connectionsEntries(
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
) {
    flavorConnectionsEntries(navigationViewModel, scaffoldPadding)
}

/**
 * The client (iOS/Android) and server (desktop) flavors have entirely different connections
 * requirements, down to the home destination itself, so every entry — including
 * [ConnectionsHomeNavKey] — is declared per flavor.
 */
expect fun EntryProviderScope<NavKey>.flavorConnectionsEntries(
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
)
