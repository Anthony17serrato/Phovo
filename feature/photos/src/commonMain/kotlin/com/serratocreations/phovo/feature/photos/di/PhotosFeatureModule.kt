package com.serratocreations.phovo.feature.photos.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun photosFeatureModule() = module {
    includes(PhotosFeatureModule().module, platformModule())
}

@Module
@ComponentScan("com.serratocreations.phovo.feature.photos")
class PhotosFeatureModule