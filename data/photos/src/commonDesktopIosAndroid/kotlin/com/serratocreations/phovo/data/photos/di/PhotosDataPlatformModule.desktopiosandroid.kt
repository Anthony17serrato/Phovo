package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.database.di.getDatabaseModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * In addition to common definitions for IOS, Desktop, & Android
 * this API provides modules that are specific to each individual platform
 */
internal expect fun getAndroidDesktopIosModules(): Module

internal actual fun getAndroidDesktopIosWasmModules(): Module = module {
    includes(getAndroidDesktopIosModules(), getDatabaseModule())

}