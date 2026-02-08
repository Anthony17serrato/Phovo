package com.serratocreations.phovo.navigation

import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.feature.connections.ui.connectionsRoutes
import com.serratocreations.phovo.feature.photos.navigation.photoRoutes
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val PhovoNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            photoRoutes()
            searchRoutes()
            connectionsRoutes()
        }
    }
}