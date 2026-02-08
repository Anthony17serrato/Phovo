package com.serratocreations.phovo.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.ui.SearchScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

@Serializable object SearchHomeNavKey: NavKey

fun PolymorphicModuleBuilder<NavKey>.searchRoutes() {
    subclass(SearchHomeNavKey::class, SearchHomeNavKey.serializer())
}

fun EntryProviderScope<NavKey>.searchEntries() {
    entry<SearchHomeNavKey> {
        SearchScreen()
    }
}