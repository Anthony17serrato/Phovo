package com.serratocreations.phovo.data.photos.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun photosDataModule() = module {
    includes(PhotosDataModule().module, platformModule())
}

@Module
@ComponentScan("com.serratocreations.phovo.data.photos")
class PhotosDataModule