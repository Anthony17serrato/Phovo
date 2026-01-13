package com.serratocreations.phovo.core.navigation

import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// Example: register ALL your NavKey implementations here
private val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        // subclass(HomeKey::class, HomeKey.serializer())
        // subclass(SettingsKey::class, SettingsKey.serializer())
        // subclass(PhotoDetailKey::class, PhotoDetailKey.serializer())
        // ... add every NavKey you use
    }
}

val PhovoNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}