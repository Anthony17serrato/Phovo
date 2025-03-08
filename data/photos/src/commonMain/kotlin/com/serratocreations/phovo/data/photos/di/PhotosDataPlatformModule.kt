package com.serratocreations.phovo.data.photos.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.serratocreations.phovo.data.photos")
internal expect class PhotosDataPlatformModule()

@Module(includes = [PhotosDataPlatformModule::class])
@ComponentScan("com.serratocreations.phovo.data.photos")
class PhotosDataModule