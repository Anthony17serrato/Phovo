package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosWasmModules(): Module

fun getPhotosDataModule(): Module = module {
    includes(getAndroidDesktopIosWasmModules())

    single {
        MediaNetworkDataSource(client = get(), logger = get())
    }
}