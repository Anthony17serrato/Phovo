package com.serratocreations.phovo.core.navigation

import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

// Example: register ALL your NavKey implementations here
val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(PhotosHomeNavKey::class, PhotosHomeNavKey.serializer())
        subclass(PhotoDetailNavKey::class, PhotoDetailNavKey.serializer())
        // ... add every NavKey you use
    }
}

val PhovoNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}