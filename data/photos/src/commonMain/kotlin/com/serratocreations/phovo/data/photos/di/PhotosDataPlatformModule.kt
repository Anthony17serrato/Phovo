package com.serratocreations.phovo.data.photos.di

import org.koin.core.module.Module
import org.koin.dsl.module

// TODO this configuration logic should be improved for ease of understanding
// gets platform module for (wasm) + (android/ios/desktop)
internal expect fun getPlatformModulesBranch1(): Module

// gets platform module for (desktop) + (android/ios/wasm)
internal expect fun getPlatformModulesBranch2(): Module

fun getPhotosDataModule(): Module = module {
    includes(getPlatformModulesBranch1(), getPlatformModulesBranch2())


}