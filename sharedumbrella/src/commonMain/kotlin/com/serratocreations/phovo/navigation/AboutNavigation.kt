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
import com.serratocreations.phovo.core.navigation.DefaultNavigationIcon
import com.serratocreations.phovo.core.navigation.NavigationViewModel
import com.serratocreations.phovo.ui.AboutScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.jetbrains.compose.resources.stringResource
import phovo.sharedumbrella.generated.resources.Res
import phovo.sharedumbrella.generated.resources.about_title

@Serializable data object AboutNavKey: NavKey

fun PolymorphicModuleBuilder<NavKey>.aboutRoutes() {
    subclass(AboutNavKey::class, AboutNavKey.serializer())
}

fun EntryProviderScope<NavKey>.aboutEntries(
    navigationViewModel: NavigationViewModel,
    scaffoldPadding: PaddingValues
) {
    entry<AboutNavKey> {
        val appBarConfig: AppBarConfig = remember {
            AppBarConfig(
                title = { Text(stringResource(Res.string.about_title)) },
                navigationIcon = {
                    DefaultNavigationIcon(navigationViewModel::goBack)
                }
            )
        }
        LaunchedEffect(navigationViewModel.state.currentKey) {
            if (navigationViewModel.state.currentKey == AboutNavKey) {
                navigationViewModel.setAppBarConfig(appBarConfig)
            }
        }
        AboutScreen(
            modifier = Modifier.padding(
                appBarConfig.calculateAdjustedPadding(scaffoldPadding)
            )
        )
    }
}
