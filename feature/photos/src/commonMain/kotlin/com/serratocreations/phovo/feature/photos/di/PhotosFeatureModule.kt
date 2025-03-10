package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.data.photos.di.PhotosDataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [PhotosDataModule::class])
@ComponentScan("com.serratocreations.phovo.feature.photos")
class PhotosFeatureModule