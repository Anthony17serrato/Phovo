package com.serratocreations.phovo.feature.connections.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

@Serializable
object ConnectionsHomeNavKey : NavKey

@Serializable
data object ConfigGettingStartedNavKey : NavKey

@Serializable
data object ConfigStorageSelectionNavKey : NavKey

fun PolymorphicModuleBuilder<NavKey>.connectionsRoutes() {
    subclass(ConnectionsHomeNavKey::class, ConnectionsHomeNavKey.serializer())
    subclass(ConfigGettingStartedNavKey::class, ConfigGettingStartedNavKey.serializer())
    subclass(ConfigStorageSelectionNavKey::class, ConfigStorageSelectionNavKey.serializer())
}
