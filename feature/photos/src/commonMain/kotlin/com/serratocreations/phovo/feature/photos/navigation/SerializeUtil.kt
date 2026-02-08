package com.serratocreations.phovo.feature.photos.navigation

import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.navigation.PhotoDetailNavKey
import com.serratocreations.phovo.core.navigation.PhotosHomeNavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder

fun PolymorphicModuleBuilder<NavKey>.photoRoutes() {
    subclass(PhotosHomeNavKey::class, PhotosHomeNavKey.serializer())
    subclass(PhotoDetailNavKey::class, PhotoDetailNavKey.serializer())
}