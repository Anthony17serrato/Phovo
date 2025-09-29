package com.serratocreations.phovo.data.photos.di

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosWasmModules(): Module

fun getPhotosDataModule(): Module = module {
    includes(getAndroidDesktopIosWasmModules())


}