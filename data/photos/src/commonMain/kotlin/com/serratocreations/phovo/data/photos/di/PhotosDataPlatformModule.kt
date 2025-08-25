package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.network.PhotosNetworkDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun getPhotosDataPlatformModule(): Module

fun getPhotosDataModule(): Module = module {
    includes(getPhotosDataPlatformModule())

    single {
        PhotosNetworkDataSource(client = get(), logger = get())
    }
}