package com.serratocreations.phovo.feature.connections.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

@Serializable
object ConnectionsHomeNavKey : NavKey

@Serializable
data object ClientOnboardingWelcomeNavKey : NavKey

@Serializable
data object ClientOnboardingPermissionPrimerNavKey : NavKey

@Serializable
data object ClientOnboardingServerDiscoveryNavKey : NavKey

@Serializable
data object ConfigGettingStartedNavKey : NavKey

@Serializable
data object ConfigStorageSelectionNavKey : NavKey

fun PolymorphicModuleBuilder<NavKey>.connectionsRoutes() {
    subclass(ConnectionsHomeNavKey::class, ConnectionsHomeNavKey.serializer())
    subclass(ClientOnboardingWelcomeNavKey::class, ClientOnboardingWelcomeNavKey.serializer())
    subclass(ClientOnboardingPermissionPrimerNavKey::class, ClientOnboardingPermissionPrimerNavKey.serializer())
    subclass(ClientOnboardingServerDiscoveryNavKey::class, ClientOnboardingServerDiscoveryNavKey.serializer())
    subclass(ConfigGettingStartedNavKey::class, ConfigGettingStartedNavKey.serializer())
    subclass(ConfigStorageSelectionNavKey::class, ConfigStorageSelectionNavKey.serializer())
}
