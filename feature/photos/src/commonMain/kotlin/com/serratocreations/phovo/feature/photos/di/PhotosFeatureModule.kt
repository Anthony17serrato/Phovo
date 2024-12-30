package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.data.photos.di.photosDataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun photosFeatureModule() = module {
    includes(PhotosFeatureModule().module, platformModule(), photosDataModule())
}

@Module
@ComponentScan("com.serratocreations.phovo.feature.photos")
class PhotosFeatureModule