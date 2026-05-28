package com.serratocreations.phovo.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
) {
    entry<SearchHomeNavKey> {
        val appBarConfig: AppBarConfig = remember {
            AppBarConfig(
                title = { Text(stringResource(Res.string.search_top_app_bar_description)) }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if(navigationViewModel.state.currentKey == SearchHomeNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }
        SearchScreen(
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
}