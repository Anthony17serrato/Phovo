package com.serratocreations.phovo.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.AppBarConfig
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.ui.SearchScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.jetbrains.compose.resources.stringResource
import phovo.phovoapp.generated.resources.Res
import phovo.phovoapp.generated.resources.search_top_app_bar_description

@Serializable object SearchHomeNavKey: NavKey

fun PolymorphicModuleBuilder<NavKey>.searchRoutes() {
    subclass(SearchHomeNavKey::class, SearchHomeNavKey.serializer())
}

fun EntryProviderScope<NavKey>.searchEntries(
    navigationViewModel: NavigationViewModel
) {
    entry<SearchHomeNavKey> {
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if(navigationViewModel.state.currentKey == SearchHomeNavKey) {
                navigationViewModel.setAppBarConfig(
                    AppBarConfig(
                        title = { Text(stringResource(Res.string.search_top_app_bar_description)) }
                    )
                )
            }
        }
        SearchScreen()
    }
}