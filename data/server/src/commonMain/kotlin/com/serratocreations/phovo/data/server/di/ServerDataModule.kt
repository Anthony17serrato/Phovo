package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.data.photos.di.getPhotosDataModule
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosModules(): Module

fun getServerDataModule(): Module = module {
    includes(getAndroidDesktopIosModules(), getPhotosDataModule())

}