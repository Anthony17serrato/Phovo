package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.data.photos.di.getPhotosDataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module(includes = [PhotosFeaturePlatformModule::class])
@ComponentScan("com.serratocreations.phovo.feature.photos")
class PhotosFeatureModule

@Module
expect class PhotosFeaturePlatformModule()

fun getPhotosFeatureModule(): org.koin.core.module.Module = module {
    includes(getPhotosDataModule())
    // TODO: Migrate from annotations back to plain Koin
}